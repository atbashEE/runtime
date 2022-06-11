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
package be.atbash.runtime;

import org.assertj.core.api.AbstractAssert;

import java.time.ZonedDateTime;

public class LogMessageAssert extends AbstractAssert<LogMessageAssert, String> {
    protected LogMessageAssert(String actual) {
        super(actual, LogMessageAssert.class);
    }


    public static LogMessageAssert assertThat(String actual) {
        return new LogMessageAssert(actual);
    }

    /**
     * Verifies the timestamp of the message.
     *
     * @param start
     * @param end
     * @return
     */
    public LogMessageAssert hasTimeStamp(ZonedDateTime start, ZonedDateTime end) {
        // Since it is possible that the sec mark is different between the timestamp used by the format
        // message and the one we take (before or after) calling the test method.
        // We check if the log message has a matching timestamp that xas taken before OR after the test method was
        // executed (since it does no take > 1 sec, this always matches one of them)
        isNotNull();
        String defaultTimestampFormat = "%1$tb %1$td, %1$tY %1$tT";
        String startTimeStamp = String.format(defaultTimestampFormat, start);
        String endTimeStamp = String.format(defaultTimestampFormat, end);

        if (!actual.startsWith(startTimeStamp) && !actual.startsWith(endTimeStamp)) {
            failWithMessage("Expected timestamp on log message does not match");
        }
        return this;
    }

    /**
     * Verifies the log message, except the time stamp (endsWith)
     *
     * @param expected
     * @return
     */
    public LogMessageAssert hasMessage(String expected) {

        isNotNull();
        if (!actual.endsWith(expected)) {
            failWithMessage("Expected log message '%s' does not match the value '%s'", expected, actual);
        }
        return this;
    }

    public LogMessageAssert hasException(Throwable throwable) {
        isNotNull();
        String[] lines = actual.split("\n");
        if (lines.length < 3) {
            failWithMessage("Expected log message does not has a Stacktrace");
        }
        if (!lines[1].equals(throwable.getClass().getName())) {
            failWithMessage("Expected log message must have Exception class name %s but found %s", throwable.getClass().getName(), lines[1]);
        }
        if (!lines[2].endsWith(throwable.getStackTrace()[0].toString())) {
            failWithMessage("Expected log message must have Stacktrace %s but found %s", throwable.getStackTrace()[0].toString(), lines[2]);
        }
        return this;
    }
}
