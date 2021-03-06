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
package be.atbash.runtime.logging.testing;

import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public final class TestLogMessages {

    private static final TestLogMessages INSTANCE = new TestLogMessages();

    private TestLogHandler handler;

    private TestLogMessages() {
    }

    void addHandler() {
        if (handler != null) {
            // Nothing to do, already initialised
            return;
        }
        Logger rootLogger = getRootLogger();

        handler = new TestLogHandler();
        rootLogger.addHandler(handler);
    }

    void clearEvents() {
        if (handler != null) {

            handler.clearEvents();
        }
    }

    private List<LoggingEvent> getEvents() {
        if (handler == null) {
            throw new IllegalStateException("The `TestLogMessages.init()` method is not called");
        }
        return handler.getLogEvents();
    }

    private Logger getRootLogger() {
        return LogManager.getLogManager().getLogger("");
    }

    public static void init() {
        INSTANCE.addHandler();
    }

    public static void init(boolean removeTestLogEvents) {
        INSTANCE.addHandler();
        INSTANCE.handler.setRemoveTestLogEvents(removeTestLogEvents);
    }

    public static void reset() {
        INSTANCE.clearEvents();
    }

    public static List<LoggingEvent> getLoggingEvents() {
        return INSTANCE.getEvents();
    }

}
