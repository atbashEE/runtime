/*
 * Copyright 2021-2023 Rudy De Busscher (https://www.atbash.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package be.atbash.runtime;

import be.atbash.runtime.core.data.RunData;
import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.deployment.AbstractDeployment;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.deployment.DeploymentPhase;
import be.atbash.runtime.core.data.exception.AtbashStartupAbortException;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventPayload;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.core.data.parameter.WatcherType;
import be.atbash.runtime.core.data.watcher.WatcherService;
import be.atbash.runtime.core.data.watcher.model.ServerMon;
import be.atbash.runtime.core.module.RuntimeObjectsManager;
import be.atbash.runtime.logging.testing.LoggingEvent;
import be.atbash.runtime.logging.testing.TestLogMessages;
import be.atbash.util.TestReflectionUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

class MainRunnerHelperTest {

    @AfterEach
    public void teardown() {
        TestLogMessages.reset();
    }

    @Test
    void handleCommandlineArguments_invalidPort() {
        TestLogMessages.init();
        MainRunnerHelper helper = new MainRunnerHelper(new String[]{"--port", "-123"});
        Assertions.assertThatThrownBy(
                () -> helper.handleCommandlineArguments()
        ).isInstanceOf(AtbashStartupAbortException.class);

        List<LoggingEvent> loggingEvents = TestLogMessages.getLoggingEvents();
        Assertions.assertThat(loggingEvents).hasSize(1);
        Assertions.assertThat(loggingEvents.get(0).getLevel()).isEqualTo(Level.SEVERE);
        Assertions.assertThat(loggingEvents.get(0).getMessage()).isEqualTo("CLI-115: The specified port '-123' is not within the range 1 - 65536.");
    }

    @Test
    void handleCommandlineArguments_wrongConfigFile() {
        TestLogMessages.init();
        MainRunnerHelper helper = new MainRunnerHelper(new String[]{"--configfile", "notExistingFile.properties"});
        Assertions.assertThatThrownBy(
                () -> helper.handleCommandlineArguments()
        ).isInstanceOf(AtbashStartupAbortException.class);

        List<LoggingEvent> loggingEvents = TestLogMessages.getLoggingEvents();
        Assertions.assertThat(loggingEvents).hasSize(1);
        Assertions.assertThat(loggingEvents.get(0).getLevel()).isEqualTo(Level.SEVERE);
        Assertions.assertThat(loggingEvents.get(0).getMessage()).isEqualTo("CLI-112: Atbash Runtime startup aborted since the configuration file 'notExistingFile.properties' does not exists or cannot be read");
    }

    @Test
    void handleCommandlineArguments_existingConfigFile() {
        TestLogMessages.init();
        MainRunnerHelper helper = new MainRunnerHelper(new String[]{"--configfile", "./src/test/resources/config.properties"});
        Assertions.assertThatNoException().isThrownBy(
                () -> helper.handleCommandlineArguments()
        );

        List<LoggingEvent> loggingEvents = TestLogMessages.getLoggingEvents();
        Assertions.assertThat(loggingEvents).isEmpty();
    }

    @Test
    void handleCommandlineArguments_wrongDataFile() {
        TestLogMessages.init();
        MainRunnerHelper helper = new MainRunnerHelper(new String[]{"--datafile", "notExistingFile.properties"});
        Assertions.assertThatThrownBy(
                () -> helper.handleCommandlineArguments()
        ).isInstanceOf(AtbashStartupAbortException.class);

        List<LoggingEvent> loggingEvents = TestLogMessages.getLoggingEvents();
        Assertions.assertThat(loggingEvents).hasSize(1);
        Assertions.assertThat(loggingEvents.get(0).getLevel()).isEqualTo(Level.SEVERE);
        Assertions.assertThat(loggingEvents.get(0).getMessage()).isEqualTo("CLI-114: Atbash Runtime startup aborted since the configuration data file 'notExistingFile.properties' does not exists or cannot be read");
    }

    @Test
    void handleCommandlineArguments_existingDataFile() {
        TestLogMessages.init();
        MainRunnerHelper helper = new MainRunnerHelper(new String[]{"--datafile", "./src/test/resources/config.properties"});
        Assertions.assertThatNoException().isThrownBy(
                () -> helper.handleCommandlineArguments()
        );

        List<LoggingEvent> loggingEvents = TestLogMessages.getLoggingEvents();
        Assertions.assertThat(loggingEvents).isEmpty();
    }

    @Test
    void handleCommandlineArguments_singleDeploymentWithContextRoot() {
        TestLogMessages.init();
        MainRunnerHelper helper = new MainRunnerHelper(new String[]{"--contextroot", "/test", "test.war"});
        Assertions.assertThatNoException().isThrownBy(
                () -> helper.handleCommandlineArguments()
        );

        List<LoggingEvent> loggingEvents = TestLogMessages.getLoggingEvents();
        Assertions.assertThat(loggingEvents).isEmpty();
    }

    @Test
    void handleCommandlineArguments_MultipleDeploymentWithContextRoot() {
        TestLogMessages.init();
        MainRunnerHelper helper = new MainRunnerHelper(new String[]{"--contextroot", "/test1,/test2", "test1.war", "test2.war"});
        Assertions.assertThatNoException().isThrownBy(
                () -> helper.handleCommandlineArguments()
        );

        List<LoggingEvent> loggingEvents = TestLogMessages.getLoggingEvents();
        Assertions.assertThat(loggingEvents).isEmpty();
    }

    @Test
    void handleCommandlineArguments_MultipleDeploymentWithWrongContextRoot() {
        TestLogMessages.init();
        MainRunnerHelper helper = new MainRunnerHelper(new String[]{"--contextroot", "/test1", "test1.war", "test2.war"});
        Assertions.assertThatThrownBy(
                () -> helper.handleCommandlineArguments()
        ).isInstanceOf(AtbashStartupAbortException.class);

        List<LoggingEvent> loggingEvents = TestLogMessages.getLoggingEvents();
        Assertions.assertThat(loggingEvents).hasSize(1);
        Assertions.assertThat(loggingEvents.get(0).getLevel()).isEqualTo(Level.SEVERE);
        Assertions.assertThat(loggingEvents.get(0).getMessage()).isEqualTo("CLI-111: Number of values for parameter --contextroot does not math number of application to be deployed");

    }

    @Test
    void stopWhenNoApplications_noApplications() throws NoSuchFieldException {
        TestLogMessages.init();

        RunData runData = new RunData();
        TestModule testModule = new TestModule(runData);

        Map<Class<?>, Module<?>> mapping = TestReflectionUtils.getValueOf(RuntimeObjectsManager.getInstance(), "runtimeObjectMapping");
        mapping.clear();
        mapping.put(WatcherService.class, testModule);
        mapping.put(RunData.class, testModule);

        MainRunnerHelper helper = new MainRunnerHelper(new String[]{});
        helper.registerRuntimeBean(new ServerMon(System.currentTimeMillis()));

        Assertions.assertThatThrownBy(() ->
                        helper.stopWhenNoApplications()
                ).isInstanceOf(AtbashStartupAbortException.class)
                .hasMessage("MODULE-001");

        List<LoggingEvent> loggingEvents = TestLogMessages.getLoggingEvents();
        Assertions.assertThat(loggingEvents).hasSize(2);
        Assertions.assertThat(loggingEvents.get(0).getLevel()).isEqualTo(Level.WARNING);
        Assertions.assertThat(loggingEvents.get(0).getMessage()).isEqualTo("CLI-105: No Applications running");
        Assertions.assertThat(loggingEvents.get(1).getLevel()).isEqualTo(Level.SEVERE);
        Assertions.assertThat(loggingEvents.get(1).getMessage()).isEqualTo("CLI-108: Atbash Runtime stopped as there are no applications deployed and Runtime is not in domain mode");

    }

    @Test
    void stopWhenNoApplications_FailedApplications() throws NoSuchFieldException {
        TestLogMessages.init();

        RunData runData = new RunData();
        TestDeployment deployment = new TestDeployment();
        deployment.forceDeploymentPhase(DeploymentPhase.FAILED);
        runData.deployed(deployment);

        TestModule testModule = new TestModule(runData);

        Map<Class<?>, Module<?>> mapping = TestReflectionUtils.getValueOf(RuntimeObjectsManager.getInstance(), "runtimeObjectMapping");
        mapping.clear();
        mapping.put(WatcherService.class, testModule);
        mapping.put(RunData.class, testModule);

        MainRunnerHelper helper = new MainRunnerHelper(new String[]{});
        helper.registerRuntimeBean(new ServerMon(System.currentTimeMillis()));

        Assertions.assertThatThrownBy(() ->
                        helper.stopWhenNoApplications()
                ).isInstanceOf(AtbashStartupAbortException.class)
                .hasMessage("MODULE-001");

        List<LoggingEvent> loggingEvents = TestLogMessages.getLoggingEvents();
        Assertions.assertThat(loggingEvents).hasSize(2);
        Assertions.assertThat(loggingEvents.get(0).getLevel()).isEqualTo(Level.WARNING);
        Assertions.assertThat(loggingEvents.get(0).getMessage()).isEqualTo("CLI-105: No Applications running");
        Assertions.assertThat(loggingEvents.get(1).getLevel()).isEqualTo(Level.SEVERE);
        Assertions.assertThat(loggingEvents.get(1).getMessage()).isEqualTo("CLI-108: Atbash Runtime stopped as there are no applications deployed and Runtime is not in domain mode");

    }

    @Test
    void stopWhenNoApplications_WithApplications() throws NoSuchFieldException {
        TestLogMessages.init();

        RunData runData = new RunData();
        TestDeployment deployment = new TestDeployment();
        deployment.forceDeploymentPhase(DeploymentPhase.READY);
        runData.deployed(deployment);

        TestModule testModule = new TestModule(runData);

        Map<Class<?>, Module<?>> mapping = TestReflectionUtils.getValueOf(RuntimeObjectsManager.getInstance(), "runtimeObjectMapping");
        mapping.clear();
        mapping.put(WatcherService.class, testModule);
        mapping.put(RunData.class, testModule);

        MainRunnerHelper helper = new MainRunnerHelper(new String[]{});
        helper.registerRuntimeBean(new ServerMon(System.currentTimeMillis()));

        helper.stopWhenNoApplications();

        List<LoggingEvent> loggingEvents = TestLogMessages.getLoggingEvents();
        Assertions.assertThat(loggingEvents).hasSize(1);
        Assertions.assertThat(loggingEvents.get(0).getLevel()).isEqualTo(Level.INFO);
        Assertions.assertThat(loggingEvents.get(0).getMessage()).isEqualTo("CLI-104: 1 Application(s) running");

    }

    private static class TestModule implements Module<Void> {

        private final WatcherService watcherService;
        private final List<String> events = new ArrayList<>();
        private final RunData runData;

        private TestModule(RunData runData) {
            this.watcherService = new WatcherService(WatcherType.OFF);
            this.runData = runData;
        }

        @Override
        public String name() {
            return null;
        }

        @Override
        public String[] dependencies() {
            return new String[0];
        }

        @Override
        public Specification[] provideSpecifications() {
            return new Specification[]{Specification.SERVLET};
        }

        @Override
        public Class<? extends Sniffer> moduleSniffer() {
            return null; // Not needed, comes through a direct call to SnifferManager.registerSniffer()
        }

        @Override
        public List<Class<?>> getRuntimeObjectTypes() {
            return null;
        }

        @Override
        public <T> T getRuntimeObject(Class<T> exposedObjectType) {
            if (WatcherService.class.equals(exposedObjectType)) {
                return (T) watcherService;
            }
            if (RunData.class.equals(exposedObjectType)) {
                return (T) runData;
            }
            return null;
        }

        @Override
        public void onEvent(EventPayload eventPayload) {
            events.add(eventPayload.getEventCode());
        }

        @Override
        public void registerDeployment(ArchiveDeployment archiveDeployment) {

        }

        public List<String> getEvents() {
            return events;
        }

        public RunData getRunData() {
            return runData;
        }

        @Override
        public void run() {

        }
    }

    static class TestDeployment extends AbstractDeployment {

        public TestDeployment() {
            super("test", "/root", new HashMap<>());
        }

        void forceDeploymentPhase(DeploymentPhase deploymentPhase) {
            this.deploymentPhase = deploymentPhase;
        }
    }
}