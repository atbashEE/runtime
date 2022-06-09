/*
 * Copyright 2021-2022 Rudy De Busscher (https://www.atbash.be)
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
package be.atbash.runtime.core.deployment;

import be.atbash.runtime.core.data.RunData;
import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.config.Config;
import be.atbash.runtime.core.data.config.Modules;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventManager;
import be.atbash.runtime.core.data.module.event.EventPayload;
import be.atbash.runtime.core.data.module.event.Events;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.core.data.parameter.WatcherType;
import be.atbash.runtime.core.data.watcher.WatcherService;
import be.atbash.runtime.core.deployment.sniffer.SingleTriggeredSniffer;
import be.atbash.runtime.core.module.RuntimeObjectsManager;
import be.atbash.util.TestReflectionUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static be.atbash.runtime.core.deployment.sniffer.SingleTriggeredSniffer.SINGLE_TRIGGERED_SNIFFER_SPECIFICATIONS;

class DeployerTest {

    private TestModule testModule;
    private WatcherService watcherService;

    @BeforeEach
    public void setup() {
        watcherService = new WatcherService(WatcherType.OFF);
    }

    @AfterEach
    public void teardown() throws NoSuchFieldException {
        EventManager.getInstance().unregisterListener(testModule);

        // Clear sniffers
        List<Sniffer> sniffers = TestReflectionUtils.getValueOf(SnifferManager.getInstance(), "sniffers");
        sniffers.clear();
        System.clearProperty(SINGLE_TRIGGERED_SNIFFER_SPECIFICATIONS);
    }

    /**
     * See if the PreDeployment and PostDeployment events are sent out.
     *
     * @throws NoSuchFieldException
     */
    @Test
    void onEvent() throws NoSuchFieldException {

        testModule = new TestModule(watcherService, false);

        // At some point we do RuntimeObjectsManager.getInstance().getExposedObject(WatcherService.class);
        //We perform mocking for that here.
        Map<Class<?>, Module<?>> mapping = TestReflectionUtils.getValueOf(RuntimeObjectsManager.getInstance(), "runtimeObjectMapping");
        mapping.clear();
        mapping.put(WatcherService.class, testModule);
        mapping.put(RunData.class, testModule);

        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        Config config = new Config();
        config.setModules(new Modules());

        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .setConfig(config)
                .build();

        List<Module> modules = Collections.singletonList(testModule);

        System.setProperty(SINGLE_TRIGGERED_SNIFFER_SPECIFICATIONS, "SERVLET,HTML");
        SnifferManager.getInstance().registerSniffer(SingleTriggeredSniffer.class);

        EventManager.getInstance().registerListener(testModule);

        Deployer deployer = new Deployer(watcherService, runtimeConfiguration, modules);


        ArchiveDeployment deployment = new ArchiveDeployment(new File("../demo/demo-servlet/target/demo-servlet.war"));
        deployer.onEvent(new EventPayload(Events.DEPLOYMENT, deployment));

        Assertions.assertThat(testModule.getEvents()).hasSize(2);
        Assertions.assertThat(testModule.getEvents()).containsExactly("PreDeployment", "PostDeployment");
        Assertions.assertThat(testModule.getRunData().getDeployments()).hasSize(1);
        Assertions.assertThat(deployment.isDeployed()).isTrue();

    }

    /**
     * See if the DeploymentDatRetriever is called..
     *
     * @throws NoSuchFieldException
     */
    @Test
    void onEvent_data() throws NoSuchFieldException {

        testModule = new TestModule(watcherService, false);

        // At some point we do RuntimeObjectsManager.getInstance().getExposedObject(WatcherService.class);
        //We perform mocking for that here.
        Map<Class<?>, Module<?>> mapping = TestReflectionUtils.getValueOf(RuntimeObjectsManager.getInstance(), "runtimeObjectMapping");
        mapping.clear();
        mapping.put(WatcherService.class, testModule);
        mapping.put(RunData.class, testModule);

        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        Config config = new Config();
        config.setModules(new Modules());

        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .setConfig(config)
                .build();

        List<Module> modules = Collections.singletonList(testModule);

        System.setProperty(SINGLE_TRIGGERED_SNIFFER_SPECIFICATIONS, "SERVLET,HTML");
        SnifferManager.getInstance().registerSniffer(SingleTriggeredSniffer.class);

        EventManager.getInstance().registerListener(testModule);

        Deployer deployer = new Deployer(watcherService, runtimeConfiguration, modules);


        ArchiveDeployment deployment = new ArchiveDeployment(new File("../demo/demo-servlet/target/demo-servlet.war"));
        deployer.onEvent(new EventPayload(Events.DEPLOYMENT, deployment));

        Assertions.assertThat(deployment.getDeploymentData("deployment-name")).isEqualTo("demo-servlet");

    }

    /**
     * test the behaviour when no module can deploy the application.
     *
     * @throws NoSuchFieldException
     */
    @Test
    void onEvent_noDeploymentModule() throws NoSuchFieldException {
        testModule = new TestModule(watcherService, false);

        // At some point we do RuntimeObjectsManager.getInstance().getExposedObject(WatcherService.class);
        //We perform mocking for that here.
        Map<Class<?>, Module<?>> mapping = TestReflectionUtils.getValueOf(RuntimeObjectsManager.getInstance(), "runtimeObjectMapping");
        mapping.clear();
        mapping.put(WatcherService.class, testModule);
        mapping.put(RunData.class, testModule);

        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        Config config = new Config();
        config.setModules(new Modules());

        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .setConfig(config)
                .build();

        List<Module> modules = Collections.singletonList(testModule);

        System.setProperty(SINGLE_TRIGGERED_SNIFFER_SPECIFICATIONS, "REST");
        // Sniffer says REST but our TestModule can only handle Servlet
        SnifferManager.getInstance().registerSniffer(SingleTriggeredSniffer.class);

        EventManager.getInstance().registerListener(testModule);

        Deployer deployer = new Deployer(watcherService, runtimeConfiguration, modules);

        ArchiveDeployment deployment = new ArchiveDeployment(new File("../demo/demo-servlet/target/demo-servlet.war"));
        deployer.onEvent(new EventPayload(Events.DEPLOYMENT, deployment));

        Assertions.assertThat(testModule.getEvents()).isEmpty();
        Assertions.assertThat(testModule.getRunData().getDeployments()).isEmpty();
        Assertions.assertThat(deployment.isDeployed()).isFalse();

    }

    /**
     * test the behaviour when there is a deployment failure
     *
     * @throws NoSuchFieldException
     */
    @Test
    void onEvent_deploymentFailure() throws NoSuchFieldException {
        testModule = new TestModule(watcherService, true);

        // At some point we do RuntimeObjectsManager.getInstance().getExposedObject(WatcherService.class);
        //We perform mocking for that here.
        Map<Class<?>, Module<?>> mapping = TestReflectionUtils.getValueOf(RuntimeObjectsManager.getInstance(), "runtimeObjectMapping");
        mapping.clear();
        mapping.put(WatcherService.class, testModule);
        mapping.put(RunData.class, testModule);

        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        Config config = new Config();
        config.setModules(new Modules());

        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .setConfig(config)
                .build();

        List<Module> modules = Collections.singletonList(testModule);

        System.setProperty(SINGLE_TRIGGERED_SNIFFER_SPECIFICATIONS, "SERVLET,HTML");
        SnifferManager.getInstance().registerSniffer(SingleTriggeredSniffer.class);

        EventManager.getInstance().registerListener(testModule);

        Deployer deployer = new Deployer(watcherService, runtimeConfiguration, modules);

        ArchiveDeployment deployment = new ArchiveDeployment(new File("../demo/demo-servlet/target/demo-servlet.war"));
        deployer.onEvent(new EventPayload(Events.DEPLOYMENT, deployment));

        Assertions.assertThat(testModule.getEvents()).hasSize(2);
        Assertions.assertThat(testModule.getEvents()).containsExactly("PreDeployment", "PostDeployment");
        Assertions.assertThat(testModule.getRunData().getDeployments()).isEmpty();
        Assertions.assertThat(deployment.isDeployed()).isFalse();

    }

    private static class TestModule implements Module<Void> {

        private final WatcherService watcherService;
        private final List<String> events = new ArrayList<>();
        private final RunData runData = new RunData();
        private final boolean failedDeployment;

        private TestModule(WatcherService watcherService, boolean failedDeployment) {
            this.watcherService = watcherService;
            this.failedDeployment = failedDeployment;
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
            if (failedDeployment) {
                archiveDeployment.setDeploymentException(new Exception());
            }

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
}