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
package be.atbash.runtime.logging.handler.formatter;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

class AnsiColorFormatterTest {

    private static final String LOGGING_CONFIG_CLASS_PROPERTY = "java.util.logging.config.class";
    private static final String FORMATTER_CLASS = TestColorFormatter.class.getCanonicalName();

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
    void color() {
        TestColorFormatter colorFormatter = new TestColorFormatter("");
        Assertions.assertThat(colorFormatter.color()).isFalse();
        Assertions.assertThat(colorFormatter.getColor(Level.INFO)).isEqualTo(AnsiColor.NOTHING);
        Assertions.assertThat(colorFormatter.getLoggerColor()).isEqualTo(AnsiColor.NOTHING);
    }

    @Test
    void color_withColor() throws IOException {
        configProperties = FORMATTER_CLASS + ".ansiColor=true";
        applyLoggingConfig();

        TestColorFormatter colorFormatter = new TestColorFormatter("");
        Assertions.assertThat(colorFormatter.color()).isTrue();
        // check for the defaults
        Assertions.assertThat(colorFormatter.getColor(Level.INFO)).isEqualTo(AnsiColor.BOLD_INTENSE_GREEN);
        Assertions.assertThat(colorFormatter.getColor(Level.WARNING)).isEqualTo(AnsiColor.BOLD_INTENSE_YELLOW);
        Assertions.assertThat(colorFormatter.getColor(Level.SEVERE)).isEqualTo(AnsiColor.BOLD_INTENSE_RED);
        Assertions.assertThat(colorFormatter.getLoggerColor()).isEqualTo(AnsiColor.BOLD_INTENSE_BLUE);
    }

    @Test
    void color_withConfig() throws IOException {
        configProperties = FORMATTER_CLASS + ".ansiColor=true\n"
                + FORMATTER_CLASS + ".infoColor=GREEN\n"
                + FORMATTER_CLASS + ".warnColor=YELLOW\n"
                + FORMATTER_CLASS + ".severeColor=RED\n"
                + FORMATTER_CLASS + ".loggerColor=BLUE\n";
        applyLoggingConfig();

        TestColorFormatter colorFormatter = new TestColorFormatter("");
        Assertions.assertThat(colorFormatter.color()).isTrue();

        Assertions.assertThat(colorFormatter.getColor(Level.INFO)).isEqualTo(AnsiColor.GREEN);
        Assertions.assertThat(colorFormatter.getColor(Level.WARNING)).isEqualTo(AnsiColor.YELLOW);
        Assertions.assertThat(colorFormatter.getColor(Level.SEVERE)).isEqualTo(AnsiColor.RED);
        Assertions.assertThat(colorFormatter.getLoggerColor()).isEqualTo(AnsiColor.BLUE);
    }

    @Test
    void color_missingActivation() throws IOException {
        configProperties = FORMATTER_CLASS + ".infoColor=GREEN\n"
                + FORMATTER_CLASS + ".warnColor=YELLOW\n"
                + FORMATTER_CLASS + ".severeColor=RED\n"
                + FORMATTER_CLASS + ".loggerColor=BLUE\n";
        applyLoggingConfig();

        TestColorFormatter colorFormatter = new TestColorFormatter("");
        Assertions.assertThat(colorFormatter.color()).isFalse();

        Assertions.assertThat(colorFormatter.getColor(Level.INFO)).isEqualTo(AnsiColor.NOTHING);
        Assertions.assertThat(colorFormatter.getColor(Level.WARNING)).isEqualTo(AnsiColor.NOTHING);
        Assertions.assertThat(colorFormatter.getColor(Level.SEVERE)).isEqualTo(AnsiColor.NOTHING);
        Assertions.assertThat(colorFormatter.getLoggerColor()).isEqualTo(AnsiColor.NOTHING);
    }

    private void applyLoggingConfig() throws IOException {
        LogManager logMgr = LogManager.getLogManager();
        System.setProperty(LOGGING_CONFIG_CLASS_PROPERTY, AnsiColorFormatterTest.TestLoggingConfig.class.getName());
        logMgr.readConfiguration();
    }

    private static class TestColorFormatter extends AnsiColorFormatter {

        public TestColorFormatter(String excludeFields) {
            super(excludeFields);
        }

        @Override
        public String format(LogRecord record) {
            return null;
        }
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