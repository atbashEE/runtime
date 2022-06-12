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

import be.atbash.json.JSONObject;
import be.atbash.json.parser.JSONParser;
import be.atbash.runtime.CustomAssertions;
import be.atbash.runtime.logging.EnhancedLogRecord;
import org.assertj.core.api.AbstractMapAssert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.logging.Level;
import java.util.logging.LogRecord;

class JSONLogFormatterTest {


    @AfterEach
    public void cleanup() {
        MDC.clear();
    }

    @Test
    void format() {
        JSONLogFormatter logFormatter = new JSONLogFormatter("");
        LogRecord record = new LogRecord(Level.INFO, "Just a message");
        record.setLoggerName("JUnit.test");
        // Ignored at level INFO
        record.setSourceClassName("be.atbash.runtime.Foo");
        record.setSourceMethodName("bar");

        ZonedDateTime zdtStart = ZonedDateTime.ofInstant(record.getInstant(), ZoneId.systemDefault());
        String message = logFormatter.format(record);
        ZonedDateTime zdtEnd = ZonedDateTime.ofInstant(record.getInstant(), ZoneId.systemDefault());

        CustomAssertions.assertThat(message).isJsonFormat();
        AbstractMapAssert mapAssert = CustomAssertions.assertThat(message).asMap();

        mapAssert.containsOnlyKeys("LoggerName",
                "LogMessage",
                "ThreadID",
                "ThreadName",
                "Level",
                "TimeMillis",
                "Timestamp",
                "LevelValue");

        mapAssert.containsEntry("LoggerName", "JUnit.test");
        mapAssert.containsEntry("LogMessage", "Just a message");
        mapAssert.containsEntry("ThreadID", "1");
        mapAssert.containsEntry("ThreadName", "main");
        mapAssert.containsEntry("Level", "INFO");
        mapAssert.containsEntry("LevelValue", "800");

        CustomAssertions.assertThat(message).hasTimeStamp(zdtStart, zdtEnd);
    }

    @Test
    void format_excludedFields() {
        JSONLogFormatter logFormatter = new JSONLogFormatter("tid,timeMillis,levelValue");
        LogRecord record = new LogRecord(Level.INFO, "Just a message");
        record.setLoggerName("JUnit.test");
        // Ignored at level INFO
        record.setSourceClassName("be.atbash.runtime.Foo");
        record.setSourceMethodName("bar");

        String message = logFormatter.format(record);

        CustomAssertions.assertThat(message).isJsonFormat();
        AbstractMapAssert mapAssert = CustomAssertions.assertThat(message).asMap();

        mapAssert.containsOnlyKeys("LoggerName",
                "LogMessage",
                "Level",
                "Timestamp");

        mapAssert.containsEntry("LoggerName", "JUnit.test");
        mapAssert.containsEntry("LogMessage", "Just a message");
        mapAssert.containsEntry("Level", "INFO");

    }

    @Test
    void format_levelFINE() {
        JSONLogFormatter logFormatter = new JSONLogFormatter("");
        LogRecord record = new LogRecord(Level.FINE, "Just a message");
        record.setLoggerName("JUnit.test");
        record.setSourceClassName("be.atbash.runtime.Foo");
        record.setSourceMethodName("bar");

        ZonedDateTime zdtStart = ZonedDateTime.ofInstant(record.getInstant(), ZoneId.systemDefault());
        String message = logFormatter.format(record);
        ZonedDateTime zdtEnd = ZonedDateTime.ofInstant(record.getInstant(), ZoneId.systemDefault());

        CustomAssertions.assertThat(message).isJsonFormat();
        AbstractMapAssert mapAssert = CustomAssertions.assertThat(message).asMap();

        mapAssert.containsOnlyKeys("LoggerName",
                "LogMessage",
                "ThreadID",
                "ClassName",
                "MethodName",
                "ThreadName",
                "Level",
                "TimeMillis",
                "Timestamp",
                "LevelValue");

        mapAssert.containsEntry("LoggerName", "JUnit.test");
        mapAssert.containsEntry("LogMessage", "Just a message");
        mapAssert.containsEntry("ThreadID", "1");
        mapAssert.containsEntry("ThreadName", "main");
        mapAssert.containsEntry("Level", "FINE");
        mapAssert.containsEntry("LevelValue", "500");
        mapAssert.containsEntry("ClassName", "be.atbash.runtime.Foo");
        mapAssert.containsEntry("MethodName", "bar");


        CustomAssertions.assertThat(message).hasTimeStamp(zdtStart, zdtEnd);
    }

    @Test
    void format_withException() {
        JSONLogFormatter logFormatter = new JSONLogFormatter("");
        LogRecord record = new LogRecord(Level.INFO, "Just a message");
        record.setLoggerName("JUnit.test");

        RuntimeException exception = new RuntimeException("RuntimeException message");
        record.setThrown(exception);

        String message = logFormatter.format(record);

        CustomAssertions.assertThat(message).isJsonFormat();
        AbstractMapAssert mapAssert = CustomAssertions.assertThat(message).asMap();

        mapAssert.containsOnlyKeys("LoggerName",
                "Throwable",
                "LogMessage",
                "ThreadID",
                "ThreadName",
                "Level",
                "TimeMillis",
                "Timestamp",
                "LevelValue");


        mapAssert.containsEntry("LoggerName", "JUnit.test");
        mapAssert.containsEntry("ThreadID", "1");
        mapAssert.containsEntry("ThreadName", "main");
        mapAssert.containsEntry("LogMessage", "Just a message");
        mapAssert.containsEntry("Level", "INFO");
        mapAssert.containsEntry("LevelValue", "800");

        Object parsed = new JSONParser().parse(message);
        Assertions.assertThat(parsed).isInstanceOf(JSONObject.class);

        JSONObject jsonObject = (JSONObject) parsed;
        Object throwable = jsonObject.get("Throwable");

        Assertions.assertThat(throwable).isInstanceOf(JSONObject.class);
        jsonObject = (JSONObject) throwable;
        Assertions.assertThat(jsonObject).containsOnlyKeys("StackTrace", "Exception");
        //Any reasonable way to test we have a stacktrace here?
        Assertions.assertThat(jsonObject).containsEntry("Exception", "RuntimeException message");

    }

    @Test
    void format_withException_nullMessage() {
        JSONLogFormatter logFormatter = new JSONLogFormatter("");
        LogRecord record = new LogRecord(Level.INFO, "");
        record.setLoggerName("JUnit.test");

        RuntimeException exception = new RuntimeException();
        record.setThrown(exception);

        String message = logFormatter.format(record);

        System.out.println(message);

        CustomAssertions.assertThat(message).isJsonFormat();
        AbstractMapAssert mapAssert = CustomAssertions.assertThat(message).asMap();

        mapAssert.containsOnlyKeys("LoggerName",
                "Throwable",
                "ThreadID",
                "ThreadName",
                "Level",
                "TimeMillis",
                "Timestamp",
                "LevelValue");
        //Log Message is not included when exception is thrown but part of Throwable

        mapAssert.containsEntry("LoggerName", "JUnit.test");
        mapAssert.containsEntry("ThreadID", "1");
        mapAssert.containsEntry("ThreadName", "main");
        mapAssert.containsEntry("Level", "INFO");
        mapAssert.containsEntry("LevelValue", "800");

        Object parsed = new JSONParser().parse(message);
        Assertions.assertThat(parsed).isInstanceOf(JSONObject.class);

        JSONObject jsonObject = (JSONObject) parsed;
        Object throwable = jsonObject.get("Throwable");

        Assertions.assertThat(throwable).isInstanceOf(JSONObject.class);
        jsonObject = (JSONObject) throwable;
        Assertions.assertThat(jsonObject).containsOnlyKeys("StackTrace");
        //Any reasonable way to test we have a stacktrace here?

    }

    @Test
    void format_mdc() {
        JSONLogFormatter logFormatter = new JSONLogFormatter("");
        LogRecord record = new LogRecord(Level.INFO, "Just a message");
        record.setLoggerName("JUnit.test");

        EnhancedLogRecord enhancedLogRecord = EnhancedLogRecord.wrap(record, true);
        MDC.put("key", "value");
        enhancedLogRecord.captureMDC();

        String message = logFormatter.format(enhancedLogRecord);

        CustomAssertions.assertThat(message).isJsonFormat();
        AbstractMapAssert mapAssert = CustomAssertions.assertThat(message).asMap();

        mapAssert.containsOnlyKeys("LoggerName",
                "LogMessage",
                "ThreadID",
                "ThreadName",
                "Level",
                "TimeMillis",
                "Timestamp",
                "LevelValue",
                "key");

        mapAssert.containsEntry("LoggerName", "JUnit.test");
        mapAssert.containsEntry("LogMessage", "Just a message");
        mapAssert.containsEntry("ThreadID", "1");
        mapAssert.containsEntry("ThreadName", "main");
        mapAssert.containsEntry("Level", "INFO");
        mapAssert.containsEntry("LevelValue", "800");
        mapAssert.containsEntry("key", "value");


    }

}