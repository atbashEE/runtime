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

import be.atbash.util.TestReflectionUtils;

import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LoggingEvent {

    private final Level level;
    private Map<String, String> mdc;
    private final Throwable throwable;
    private final String message;
    private final List<Object> arguments;
    private final String creatingLogger;
    private final Instant timestamp;
    private final ResourceBundle resourceBundle;

    public LoggingEvent(LogRecord logRecord) {
        level = logRecord.getLevel();

        message = logRecord.getMessage();
        Object[] parameters = logRecord.getParameters();
        arguments = parameters == null ? Collections.emptyList() : Arrays.asList(parameters);
        creatingLogger = logRecord.getLoggerName();
        throwable = logRecord.getThrown();
        timestamp = logRecord.getInstant();
        resourceBundle = logRecord.getResourceBundle();

        // We cannot add dependency to logging-core as that would result in circular dependency
        try {
            Map<String, String> mdc = TestReflectionUtils.getValueOf(logRecord, "mdc");
            this.mdc = mdc;
        } catch (NoSuchFieldException e) {
            // When Exception, it is not a EnhancedLogRecord
            mdc = Collections.emptyMap();
        }

    }

    public Level getLevel() {
        return level;
    }

    public Map<String, String> getMdc() {
        return mdc;
    }

    public Optional<Throwable> getThrowable() {
        return Optional.ofNullable(throwable);
    }

    public String getMessage() {
        return message;
    }

    public List<Object> getArguments() {
        return arguments;
    }

    public String getCreatingLogger() {
        return creatingLogger;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }
}
