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
package be.atbash.runtime.logging.slf4j.jul;

import be.atbash.runtime.logging.testing.LoggingEvent;
import be.atbash.runtime.logging.testing.TestLogMessages;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.List;
import java.util.Properties;

class JULLoggerFactoryTest {
    // Some functionality of the class can't be tested (like the Environment variable for warnings and getting rootLgger.

    @AfterEach
    public void teardown() {
        TestLogMessages.reset();
    }

    @Test
    void getLogger() {

        JULLoggerFactory factory = new JULLoggerFactory();
        Logger logger = factory.getLogger(JULLoggerFactoryTest.class.getName());
        Assertions.assertThat(logger).isNotNull();
        Assertions.assertThat(logger).isInstanceOf(JULLoggerAdapter.class);
    }


    @Test
    void getLogger_supportResourceBundle() {
        TestLogMessages.init();
        JULLoggerFactory factory = new JULLoggerFactory();
        Logger logger = factory.getLogger(JULLoggerFactoryTest.class.getName());
        Assertions.assertThat(logger).isNotNull();
        logger.info("JUNIT-001", "Parameter Value");

        List<LoggingEvent> loggingEvents = TestLogMessages.getLoggingEvents();

        Assertions.assertThat(loggingEvents).hasSize(1);
        // Test Log Handler just captures the LogRecord, not expanding the message from resource bundle.
        Assertions.assertThat(loggingEvents.get(0).getMessage()).isEqualTo("JUNIT-001");
        Assertions.assertThat(loggingEvents.get(0).getArguments()).containsExactly("Parameter Value");
        Assertions.assertThat(loggingEvents.get(0).getResourceBundle().getBaseBundleName()).isEqualTo("msg." + JULLoggerFactoryTest.class.getName());
    }

    @Test
    void getLogger_cached() {

        JULLoggerFactory factory = new JULLoggerFactory();
        Logger logger1 = factory.getLogger(JULLoggerFactoryTest.class.getName());
        Logger logger2 = factory.getLogger(JULLoggerFactoryTest.class.getName());

        Assertions.assertThat(logger1 == logger2).isTrue(); // Although technically it is possible that there is already a GC happened.
    }

    @Test
    void getLogger_noResourceBundle() {
        TestLogMessages.init();
        JULLoggerFactory factory = new JULLoggerFactory();
        Logger logger = factory.getLogger("someLogger");
        Assertions.assertThat(logger).isNotNull();
        logger.info("JUNIT-001", "Parameter Value");

        List<LoggingEvent> loggingEvents = TestLogMessages.getLoggingEvents();

        Assertions.assertThat(loggingEvents).hasSize(1);
        // Test Log Handler just captures the LogRecord, not expanding the message from resource bundle.
        Assertions.assertThat(loggingEvents.get(0).getMessage()).isEqualTo("JUNIT-001");
        Assertions.assertThat(loggingEvents.get(0).getArguments()).containsExactly("Parameter Value");
        Assertions.assertThat(loggingEvents.get(0).getResourceBundle()).isNull();
    }

}