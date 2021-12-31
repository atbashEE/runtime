/*
 * Copyright 2021 Rudy De Busscher (https://www.atbash.be)
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
package be.atbash.runtime.logging;

import be.atbash.runtime.logging.handler.RuntimeConsoleHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static be.atbash.runtime.logging.LoggingUtil.SYSTEM_PROPERTY_FILE_LOGGING;
import static be.atbash.runtime.logging.LoggingUtil.SYSTEM_PROPERTY_LOGGING_VERBOSE;
import static org.assertj.core.api.Assertions.assertThat;

class LoggingUtilTest {


    @AfterEach
    public void cleanup() {
        System.clearProperty(LoggingUtil.SYSTEM_PROPERTY_LOGGING_VERBOSE);
        System.clearProperty(LoggingUtil.SYSTEM_PROPERTY_LOGGING_CONSOLE);
        System.clearProperty(SYSTEM_PROPERTY_FILE_LOGGING);
    }

    @Test
    void handleConsoleHandlerLogic_propertySet() {
        System.setProperty(LoggingUtil.SYSTEM_PROPERTY_LOGGING_CONSOLE, "true");

        Properties properties = new Properties();
        properties.setProperty("handlers", "some.handler.class");
        addExtraProperties(properties);
        LoggingUtil.handleConsoleHandlerLogic(properties);

        assertThat(properties.getProperty("handlers").split(",")).hasSize(2);
        assertThat(properties.getProperty("handlers")).contains(RuntimeConsoleHandler.class.getName());
        assertThat(properties.getProperty("handlers")).contains("some.handler.class");
        testExtraParameters(properties);
    }


    private void addExtraProperties(Properties properties) {
        properties.setProperty("foo", "bar");
        properties.setProperty("junit", "runtime");
    }

    private void testExtraParameters(Properties properties) {
        assertThat(properties.stringPropertyNames()).contains("handlers", "foo", "junit");
        assertThat(properties.getProperty("foo")).isEqualTo("bar");
        assertThat(properties.getProperty("junit")).isEqualTo("runtime");
    }

    @Test
    void handleConsoleHandlerLogic_propertySet_hasValue() {
        System.setProperty(LoggingUtil.SYSTEM_PROPERTY_LOGGING_CONSOLE, "true");

        Properties properties = new Properties();
        properties.setProperty("handlers", "some.handler.class," + RuntimeConsoleHandler.class.getName());
        addExtraProperties(properties);
        LoggingUtil.handleConsoleHandlerLogic(properties);

        assertThat(properties.getProperty("handlers").split(",")).hasSize(2);
        assertThat(properties.getProperty("handlers")).contains(RuntimeConsoleHandler.class.getName());
        assertThat(properties.getProperty("handlers")).contains("some.handler.class");
    }

    @Test
    void handleConsoleHandlerLogic_propertyNotSet_noValue() {
        System.setProperty(LoggingUtil.SYSTEM_PROPERTY_LOGGING_CONSOLE, "false");

        Properties properties = new Properties();
        properties.setProperty("handlers", "some.handler.class");
        addExtraProperties(properties);
        LoggingUtil.handleConsoleHandlerLogic(properties);

        assertThat(properties.getProperty("handlers").split(",")).hasSize(1);
        assertThat(properties.getProperty("handlers")).doesNotContain(RuntimeConsoleHandler.class.getName());
        assertThat(properties.getProperty("handlers")).contains("some.handler.class");
    }

    @Test
    void handleConsoleHandlerLogic_propertyNotSet_hasValue() {
        System.setProperty(LoggingUtil.SYSTEM_PROPERTY_LOGGING_CONSOLE, "false");

        Properties properties = new Properties();
        properties.setProperty("handlers", "some.handler.class," + RuntimeConsoleHandler.class.getName());
        addExtraProperties(properties);
        LoggingUtil.handleConsoleHandlerLogic(properties);

        assertThat(properties.getProperty("handlers").split(",")).hasSize(1);
        assertThat(properties.getProperty("handlers")).doesNotContain(RuntimeConsoleHandler.class.getName());
        assertThat(properties.getProperty("handlers")).contains("some.handler.class");
    }

    @Test
    void handleConsoleHandlerLogic() {

        Properties properties = new Properties();
        properties.setProperty("handlers", "some.handler.class");
        addExtraProperties(properties);
        LoggingUtil.handleConsoleHandlerLogic(properties);

        assertThat(properties.getProperty("handlers").split(",")).hasSize(1);
        assertThat(properties.getProperty("handlers")).contains("some.handler.class");
        testExtraParameters(properties);
    }


    @Test
    void handleLogToFileHandlerLogic() {

        Properties properties = new Properties();
        properties.setProperty("handlers", "some.handler.class,be.atbash.runtime.logging.handler.LogFileHandler");
        addExtraProperties(properties);

        LoggingUtil.handleLogToFileHandlerLogic(properties);
        assertThat(properties.getProperty("handlers").split(",")).hasSize(2);
        assertThat(properties.getProperty("handlers")).contains("some.handler.class");
        assertThat(properties.getProperty("handlers")).contains("be.atbash.runtime.logging.handler.LogFileHandler");
        testExtraParameters(properties);

    }

    @Test
    void handleLogToFileHandlerLogic_propertyUnset() {
        System.setProperty(SYSTEM_PROPERTY_FILE_LOGGING, "false");

        Properties properties = new Properties();
        properties.setProperty("handlers", "some.handler.class,be.atbash.runtime.logging.handler.LogFileHandler");
        addExtraProperties(properties);

        LoggingUtil.handleLogToFileHandlerLogic(properties);
        assertThat(properties.getProperty("handlers").split(",")).hasSize(1);
        assertThat(properties.getProperty("handlers")).contains("some.handler.class");
        testExtraParameters(properties);

    }

    @Test
    void handleLogToFileHandlerLogic_propertyUnset_noValue() {
        System.setProperty(SYSTEM_PROPERTY_FILE_LOGGING, "false");

        Properties properties = new Properties();
        properties.setProperty("handlers", "some.handler.class");
        addExtraProperties(properties);

        LoggingUtil.handleLogToFileHandlerLogic(properties);
        assertThat(properties.getProperty("handlers").split(",")).hasSize(1);
        assertThat(properties.getProperty("handlers")).contains("some.handler.class");
        testExtraParameters(properties);

    }

    @Test
    void handleLogToFileHandlerLogic_propertySet_noValue() {
        System.setProperty(SYSTEM_PROPERTY_FILE_LOGGING, "true");

        Properties properties = new Properties();
        properties.setProperty("handlers", "some.handler.class,be.atbash.runtime.logging.handler.LogFileHandler");
        addExtraProperties(properties);

        LoggingUtil.handleLogToFileHandlerLogic(properties);
        assertThat(properties.getProperty("handlers").split(",")).hasSize(2);
        assertThat(properties.getProperty("handlers")).contains("some.handler.class");
        assertThat(properties.getProperty("handlers")).contains("be.atbash.runtime.logging.handler.LogFileHandler");
        testExtraParameters(properties);

    }

    @Test
    void handleLogToFileHandlerLogic_propertySet_hasValue() {
        System.setProperty(SYSTEM_PROPERTY_FILE_LOGGING, "true");

        Properties properties = new Properties();
        properties.setProperty("handlers", "some.handler.class,be.atbash.runtime.logging.handler.LogFileHandler");
        addExtraProperties(properties);

        LoggingUtil.handleLogToFileHandlerLogic(properties);
        assertThat(properties.getProperty("handlers").split(",")).hasSize(2);
        assertThat(properties.getProperty("handlers")).contains("some.handler.class");
        assertThat(properties.getProperty("handlers")).contains("be.atbash.runtime.logging.handler.LogFileHandler");
        testExtraParameters(properties);

    }


    @Test
    void handleVerboseLogic() {
        System.setProperty(SYSTEM_PROPERTY_LOGGING_VERBOSE, "true");
        Properties properties = new Properties();
        LoggingUtil.handleVerboseLogic(properties);

        assertThat(properties.getProperty("be.atbash.runtime.level")).isEqualTo("ALL");

    }

    @Test
    void handleVerboseLogic_propertyNotSet() {
        System.setProperty(SYSTEM_PROPERTY_LOGGING_VERBOSE, "false");
        Properties properties = new Properties();
        LoggingUtil.handleVerboseLogic(properties);

        assertThat(properties.stringPropertyNames()).doesNotContain("be.atbash.runtime.level");

    }

    @Test
    void handleVerboseLogic_overwrite() {
        System.setProperty(SYSTEM_PROPERTY_LOGGING_VERBOSE, "true");
        Properties properties = new Properties();
        properties.setProperty("be.atbash.runtime.level", "INFO");
        LoggingUtil.handleVerboseLogic(properties);

        assertThat(properties.getProperty("be.atbash.runtime.level")).isEqualTo("ALL");

    }

}