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

import be.atbash.runtime.CustomAssertions;
import be.atbash.runtime.logging.EnhancedLogRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.logging.Level;
import java.util.logging.LogRecord;

class SimpleLogFormatterTest {

    @AfterEach
    public void cleanup() {
        MDC.clear();
    }

    @Test
    void format() {
        SimpleLogFormatter logFormatter = new SimpleLogFormatter();
        LogRecord record = new LogRecord(Level.INFO, "Just a message");
        record.setLoggerName("JUnit.test");

        ZonedDateTime zdtStart = ZonedDateTime.ofInstant(record.getInstant(), ZoneId.systemDefault());
        String message = logFormatter.format(record);
        ZonedDateTime zdtEnd = ZonedDateTime.ofInstant(record.getInstant(), ZoneId.systemDefault());

        CustomAssertions.assertThat(message).hasMessage("JUnit.test INFO: Just a message\n");
        CustomAssertions.assertThat(message).hasTimeStamp(zdtStart, zdtEnd);
    }

    @Test
    void format_withSourceClassAndName() {
        SimpleLogFormatter logFormatter = new SimpleLogFormatter();
        LogRecord record = new LogRecord(Level.INFO, "Just a message");
        record.setSourceClassName("be.atbash.runtime.Foo");
        record.setSourceMethodName("bar");

        String message = logFormatter.format(record);

        CustomAssertions.assertThat(message).hasMessage("be.atbash.runtime.Foo#bar INFO: Just a message\n");
    }

    @Test
    void format_withException() {
        SimpleLogFormatter logFormatter = new SimpleLogFormatter();
        LogRecord record = new LogRecord(Level.INFO, "Internal error");
        record.setLoggerName("JUnit.test");
        RuntimeException exception = new RuntimeException();
        record.setThrown(exception);

        String message = logFormatter.format(record);

        CustomAssertions.assertThat(message).hasException(exception);

    }

    @Test
    void format_withMDC() {
        SimpleLogFormatter logFormatter = new SimpleLogFormatter();
        LogRecord record = new LogRecord(Level.INFO, "Log message contains Context info");
        record.setLoggerName("JUnit.test");

        EnhancedLogRecord enhancedLogRecord = EnhancedLogRecord.wrap(record, true);
        MDC.put("key", "value");
        enhancedLogRecord.captureMDC();

        String message = logFormatter.format(enhancedLogRecord);

        CustomAssertions.assertThat(message).hasMessage("JUnit.test INFO: [key=value]Log message contains Context info\n");

    }

    @Test
    void format_withMDC_multiple() {
        SimpleLogFormatter logFormatter = new SimpleLogFormatter();
        LogRecord record = new LogRecord(Level.INFO, "Log message contains Context info");
        record.setLoggerName("JUnit.test");

        EnhancedLogRecord enhancedLogRecord = EnhancedLogRecord.wrap(record, true);
        MDC.put("key", "value");
        MDC.put("uid", "UniqueValue");
        enhancedLogRecord.captureMDC();

        String message = logFormatter.format(enhancedLogRecord);

        CustomAssertions.assertThat(message).hasMessage("JUnit.test INFO: [uid=UniqueValue, key=value]Log message contains Context info\n");

    }
}