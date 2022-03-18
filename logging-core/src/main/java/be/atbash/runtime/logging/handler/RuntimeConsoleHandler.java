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
package be.atbash.runtime.logging.handler;

import be.atbash.runtime.logging.LoggingUtil;

import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

/**
 * We need a specific ConsoleHandler to remove the early logged messages as they are already on console.
 */
public class RuntimeConsoleHandler extends StreamHandler {

    /**
     * Create a {@code ConsoleHandler} for {@code System.err}.
     * <p>
     * The {@code ConsoleHandler} is configured based on
     * {@code LogManager} properties (or their default values).
     */
    public RuntimeConsoleHandler() {
        // configure with specific defaults for ConsoleHandler
        super(LoggingUtil.oStdErrBackup, new SimpleFormatter());
    }

    /**
     * Publish a {@code LogRecord}.
     * <p>
     * The logging request was made initially to a {@code Logger} object,
     * which initialized the {@code LogRecord} and forwarded it here.
     *
     * @param record description of the log event. A null record is
     *               silently ignored and is not published
     */
    @Override
    public synchronized void publish(LogRecord record) {
        if (record.getMessage().startsWith("*")) {
            // This is a message from the EarlyLogHandler and should not be written
            //to console again (is already)
            return;
        }
        super.publish(record);
        flush();
    }

    /**
     * Override {@code StreamHandler.close} to do a flush but not
     * to close the output stream.  That is, we do <b>not</b>
     * close {@code System.err}.
     */
    @Override
    public synchronized void close() {
        flush();
    }

}
