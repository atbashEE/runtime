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
package be.atbash.runtime.logging.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.LogManager;

class LogUtilTest {

    private static final String LOGGING_CONFIG_CLASS_PROPERTY = "java.util.logging.config.class";

    private static String configProperties;

    @AfterEach
    public void cleanup() {
        System.clearProperty(LOGGING_CONFIG_CLASS_PROPERTY);
        LogManager logMgr = LogManager.getLogManager();
        try {
            logMgr.readConfiguration();
        } catch (IOException e) {
            Assertions.fail(e.getMessage());
        }
    }

    @Test
    void getLogPropertyKey_withoutPrefix() {
        String key = LogUtil.getLogPropertyKey("test");
        Assertions.assertThat(key).isEqualTo("be.atbash.runtime.logging.handler.LogFileHandler.test");
    }

    @Test
    void getLogPropertyKey_withPrefix() {
        String key = LogUtil.getLogPropertyKey("foo.bar");
        Assertions.assertThat(key).isEqualTo("foo.bar");
    }

    @Test
    void getBooleanProperty() throws IOException {

        configProperties = "prop.bool=true";
        applyLoggingConfig();

        boolean booleanProperty = LogUtil.getBooleanProperty("prop.bool", false);
        Assertions.assertThat(booleanProperty).isTrue();
    }

    @Test
    void getBooleanProperty_uppercase() throws IOException {

        configProperties = "prop.bool=TRUE";
        applyLoggingConfig();

        boolean booleanProperty = LogUtil.getBooleanProperty("prop.bool", false);
        Assertions.assertThat(booleanProperty).isTrue();
    }

    @Test
    void getBooleanProperty_one() throws IOException {

        configProperties = "prop.bool=1";
        applyLoggingConfig();

        boolean booleanProperty = LogUtil.getBooleanProperty("prop.bool", false);
        Assertions.assertThat(booleanProperty).isTrue();
    }

    @Test
    void getBooleanProperty_default() throws IOException {

        configProperties = "";
        applyLoggingConfig();

        boolean booleanProperty = LogUtil.getBooleanProperty("prop.bool", false);
        Assertions.assertThat(booleanProperty).isFalse();
    }

    @Test
    void getBooleanProperty_random() throws IOException {

        configProperties = "prop.bool=valid";
        applyLoggingConfig();

        boolean booleanProperty = LogUtil.getBooleanProperty("prop.bool", false);
        Assertions.assertThat(booleanProperty).isFalse();
    }

    @Test
    void getBooleanProperty_false() throws IOException {

        configProperties = "prop.bool=false";
        applyLoggingConfig();

        boolean booleanProperty = LogUtil.getBooleanProperty("prop.bool", true);
        Assertions.assertThat(booleanProperty).isFalse();
    }

    @Test
    void getBooleanProperty_false_uppercase() throws IOException {

        configProperties = "prop.bool=FALSE";
        applyLoggingConfig();

        boolean booleanProperty = LogUtil.getBooleanProperty("prop.bool", true);
        Assertions.assertThat(booleanProperty).isFalse();
    }

    @Test
    void getBooleanProperty_false_zero() throws IOException {

        configProperties = "prop.bool=0";
        applyLoggingConfig();

        boolean booleanProperty = LogUtil.getBooleanProperty("prop.bool", true);
        Assertions.assertThat(booleanProperty).isFalse();
    }

    private void applyLoggingConfig() throws IOException {
        LogManager logMgr = LogManager.getLogManager();
        System.setProperty(LOGGING_CONFIG_CLASS_PROPERTY, TestLoggingConfig.class.getName());
        logMgr.readConfiguration();
    }


    @Test
    void getLongProperty() throws IOException {
        configProperties = "prop.long=12345";
        applyLoggingConfig();

        long longProperty = LogUtil.getLongProperty("prop.long", 54321);
        Assertions.assertThat(longProperty).isEqualTo(12345);

    }

    @Test
    void getLongProperty_default() throws IOException {
        configProperties = "";
        applyLoggingConfig();

        long longProperty = LogUtil.getLongProperty("prop.long", 54321);
        Assertions.assertThat(longProperty).isEqualTo(54321);

    }


    @Test
    void getLongProperty_notNumber() throws IOException {
        configProperties = "prop.long=notNumber";
        applyLoggingConfig();

        long longProperty = LogUtil.getLongProperty("prop.long", 54321);
        Assertions.assertThat(longProperty).isEqualTo(54321);
        // FIXME there is no handler in configProperties so we don't have any output from Logger.

    }


    @Test
    void getIntProperty() throws IOException {
        configProperties = "prop.int=12345";
        applyLoggingConfig();

        int intProperty = LogUtil.getIntProperty("prop.int", 54321);
        Assertions.assertThat(intProperty).isEqualTo(12345);

    }

    @Test
    void getIntProperty_default() throws IOException {
        configProperties = "";
        applyLoggingConfig();

        int intProperty = LogUtil.getIntProperty("prop.int", 54321);
        Assertions.assertThat(intProperty).isEqualTo(54321);

    }

    @Test
    void getIntProperty_notNumber() throws IOException {
        configProperties = "prop.int=NotNumber";
        applyLoggingConfig();

        int intProperty = LogUtil.getIntProperty("prop.int", 54321);
        Assertions.assertThat(intProperty).isEqualTo(54321);

    }

    public static class TestLoggingConfig {

        public TestLoggingConfig() {
            LogManager logMgr = LogManager.getLogManager();
            InputStream data = new ByteArrayInputStream(configProperties.getBytes(StandardCharsets.UTF_8));
            try {
                logMgr.readConfiguration(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}