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
package be.atbash.runtime.logging.slf4j;

import be.atbash.runtime.logging.slf4j.jul.JULLoggerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;
import org.slf4j.event.Level;
import org.slf4j.event.LoggingEvent;

import java.util.Arrays;
import java.util.List;

/**
 * This is not a complete implementation of the {@code LoggingEvent} interface but one that is enough
 * for the retrieval of the message by {code {@link be.atbash.runtime.logging.LoggingUtil#formatMessage(LoggingEvent)}},
 * just as a normal logger would do.
 */
public class RuntimeLoggingEvent implements LoggingEvent {

    private final JULLoggerAdapter logger;
    private final String message;

    private List<Object> arguments;


    private Throwable throwable;
    private String threadName;
    private long timeStamp;


    public RuntimeLoggingEvent(String loggerName, String message, Object... parameters) {
        this(LoggerFactory.getLogger(loggerName), message, parameters);
    }

    public RuntimeLoggingEvent(Logger logger, String message, Object... parameters) {
        if (!(logger instanceof JULLoggerAdapter)) {
            throw new IllegalArgumentException();  // FIXME
        }

        this.logger = (JULLoggerAdapter) logger;
        this.message = message;
        this.arguments = Arrays.asList(parameters);
    }

    @Override
    public List<Marker> getMarkers() {
        return null;  // return null is allowed
    }

    @Override
    public List<Object> getArguments() {
        return arguments;
    }

    @Override
    public Object[] getArgumentArray() {
        if (arguments == null) {
            return null;
        }
        return arguments.toArray();
    }


    @Override
    public List<KeyValuePair> getKeyValuePairs() {
        return null;  // null is allowed as return value.
    }

    public void setThrowable(Throwable cause) {
        this.throwable = cause;
    }

    @Override
    public Level getLevel() {
        return Level.INFO;  // some arbitrary value to satisfy interface.
    }

    @Override
    public String getLoggerName() {
        return logger.getName();
    }

    public JULLoggerAdapter getLoggerAdapter() {
        return logger;
    }

    @Override
    public String getMessage() {
        return message;
    }


    @Override
    public Throwable getThrowable() {
        return throwable;
    }

    public String getThreadName() {
        return threadName;
    }

    public long getTimeStamp() {
        return timeStamp;
    }
}