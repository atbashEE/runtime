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
package be.atbash.runtime.logging.testing;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class TestLogHandler extends Handler {

    private final List<LoggingEvent> logEvents = new CopyOnWriteArrayList<>();

    private static final List<String> TEST_LOGGERS = List.of("org.mockserver.log.MockServerEventLog");

    private boolean removeTestLogEvents;

    @Override
    public void publish(LogRecord record) {
        if (removeTestLogEvents) {
            if (TEST_LOGGERS.contains(record.getLoggerName())) {
                return;
            }
        }
        logEvents.add(new TestingLoggingEvent(record));
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {

    }

    public void setRemoveTestLogEvents(boolean removeTestLogEvents) {
        this.removeTestLogEvents = removeTestLogEvents;
    }

    public List<LoggingEvent> getLogEvents() {
        return logEvents;
    }

    public void clearEvents() {
        logEvents.clear();
    }
}
