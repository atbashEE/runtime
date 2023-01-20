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

import be.atbash.runtime.core.data.exception.AtbashStartupAbortException;
import be.atbash.runtime.logging.testing.LoggingEvent;
import be.atbash.runtime.logging.testing.TestLogMessages;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
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
}