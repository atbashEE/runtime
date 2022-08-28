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
import org.assertj.core.api.AbstractMapAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.logging.Level;
import java.util.logging.LogRecord;

class ODLLogFormatterTest {
    // The color feature is very difficult to test since it depends on thr LogManager
    // and is a Singleton that you can only define once.

    @AfterEach
    public void cleanup() {
        MDC.clear();
    }

    @Test
    void format() {
        ODLLogFormatter logFormatter = new ODLLogFormatter("");
        LogRecord record = new LogRecord(Level.INFO, "Just a message");
        record.setLoggerName("JUnit.test");
        // Ignored at level INFO
        record.setSourceClassName("be.atbash.runtime.Foo");
        record.setSourceMethodName("bar");

        ZonedDateTime zdtStart = ZonedDateTime.ofInstant(record.getInstant(), ZoneId.systemDefault());
        String message = logFormatter.format(record);
        ZonedDateTime zdtEnd = ZonedDateTime.ofInstant(record.getInstant(), ZoneId.systemDefault());

        CustomAssertions.assertThat(message).isODLFormat();

        CustomAssertions.assertThat(message).hasMessage("[INFO] [] [JUnit.test] Just a message\n");

        AbstractMapAssert mapAssert = CustomAssertions.assertThat(message).asMap();
        mapAssert.containsOnlyKeys("levelValue", "tid", "timeMillis");

        mapAssert.containsEntry("tid", "_ThreadID=1 _ThreadName=main");
        mapAssert.containsEntry("levelValue", "800");

        CustomAssertions.assertThat(message).hasTimeStamp(zdtStart, zdtEnd);
    }

    @Test
    void format_levelFINE() {
        ODLLogFormatter logFormatter = new ODLLogFormatter("");
        LogRecord record = new LogRecord(Level.FINE, "Just a message");
        record.setLoggerName("JUnit.test");
        // Ignored at level INFO
        record.setSourceClassName("be.atbash.runtime.Foo");
        record.setSourceMethodName("bar");

        ZonedDateTime zdtStart = ZonedDateTime.ofInstant(record.getInstant(), ZoneId.systemDefault());
        String message = logFormatter.format(record);
        ZonedDateTime zdtEnd = ZonedDateTime.ofInstant(record.getInstant(), ZoneId.systemDefault());

        CustomAssertions.assertThat(message).isODLFormat();
        CustomAssertions.assertThat(message).hasMessage("[FINE] [] [JUnit.test] Just a message\n");

        AbstractMapAssert mapAssert = CustomAssertions.assertThat(message).asMap();
        mapAssert.containsOnlyKeys("CLASSNAME", "METHODNAME", "tid", "levelValue","timeMillis");

        mapAssert.containsEntry("CLASSNAME", "be.atbash.runtime.Foo");
        mapAssert.containsEntry("METHODNAME", "bar");
        mapAssert.containsEntry("tid", "_ThreadID=1 _ThreadName=main");
        mapAssert.containsEntry("levelValue", "500");

        CustomAssertions.assertThat(message).hasTimeStamp(zdtStart, zdtEnd);
    }

    @Test
    void format_excludeFields() {
        ODLLogFormatter logFormatter = new ODLLogFormatter("tid,timeMillis,levelValue");
        LogRecord record = new LogRecord(Level.INFO, "Just a message");
        record.setLoggerName("JUnit.test");

        String message = logFormatter.format(record);

        CustomAssertions.assertThat(message).isODLFormat();
        CustomAssertions.assertThat(message).hasMessage("[INFO] [] [JUnit.test] Just a message\n");

        AbstractMapAssert mapAssert = CustomAssertions.assertThat(message).asMap();
        mapAssert.isEmpty();

    }


    @Test
    void format_withException() {
        ODLLogFormatter logFormatter = new ODLLogFormatter("");
        LogRecord record = new LogRecord(Level.INFO, "Just a message");
        record.setLoggerName("JUnit.test");
        // Ignored at level INFO
        Throwable exception = new RuntimeException("Exception message");
        record.setThrown(exception);

        ZonedDateTime zdtStart = ZonedDateTime.ofInstant(record.getInstant(), ZoneId.systemDefault());
        String message = logFormatter.format(record);
        ZonedDateTime zdtEnd = ZonedDateTime.ofInstant(record.getInstant(), ZoneId.systemDefault());

        CustomAssertions.assertThat(message).isODLFormat();
        CustomAssertions.assertThat(message).hasMessage("[INFO] [] [JUnit.test] Just a message\n");

        AbstractMapAssert mapAssert = CustomAssertions.assertThat(message).asMap();
        mapAssert.containsOnlyKeys("levelValue", "tid", "timeMillis");

        CustomAssertions.assertThat(message).hasTimeStamp(zdtStart, zdtEnd);
    }

    @Test
    void format_withMDC() {
        ODLLogFormatter logFormatter = new ODLLogFormatter("");
        LogRecord record = new LogRecord(Level.INFO, "Just a message");
        record.setLoggerName("JUnit.test");

        EnhancedLogRecord enhancedLogRecord = EnhancedLogRecord.wrap(record, true);
        MDC.put("key", "value");
        enhancedLogRecord.captureMDC();


        String message = logFormatter.format(enhancedLogRecord);


        CustomAssertions.assertThat(message).isODLFormat();
        CustomAssertions.assertThat(message).hasMessage("[INFO] [] [JUnit.test] Just a message\n");

        AbstractMapAssert mapAssert = CustomAssertions.assertThat(message).asMap();
        mapAssert.containsOnlyKeys("levelValue", "tid", "timeMillis", "key");

        mapAssert.containsEntry("key", "value");
    }

}