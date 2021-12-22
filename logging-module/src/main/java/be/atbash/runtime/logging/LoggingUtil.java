/*
 * Copyright 2021 Rudy De Busscher (https://www.atbash.be)
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
package be.atbash.runtime.logging;

import java.util.Properties;

// Based on Payara code

public final class LoggingUtil {

    public static final String LOGTOCONSOLE_PROPERTY = "be.atbash.runtime.logging.handler.LogFileHandler.logtoConsole";
    public static final String SYSTEM_PROPERTY_LOGGING_CONSOLE = "runtime.logging.console";
    public static final String SYSTEM_PROPERTY_LOGGING_VERBOSE = "runtime.logging.verbose";

    private static final String HANDLERS = "handlers";

    private LoggingUtil() {
    }

    public static void handleConsoleHandlerLogic(Properties loggingProperties) {
        boolean logToConsole = isLogToConsole();

        if (logToConsole) {
            String handlers = loggingProperties.getProperty(HANDLERS);
            loggingProperties.setProperty(HANDLERS, handlers + "," + RuntimeConsoleHandler.class.getName());
        }

        loggingProperties.put(LOGTOCONSOLE_PROPERTY, Boolean.toString(logToConsole));
    }

    public static boolean isLogToConsole() {
        return Boolean.parseBoolean(System.getProperty(LoggingUtil.SYSTEM_PROPERTY_LOGGING_CONSOLE, "false"));
    }

    public static boolean isVerbose() {
        return Boolean.parseBoolean(System.getProperty(LoggingUtil.SYSTEM_PROPERTY_LOGGING_VERBOSE, "false"));
    }

}

