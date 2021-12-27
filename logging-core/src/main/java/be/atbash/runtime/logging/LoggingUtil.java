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

import be.atbash.runtime.logging.handler.RuntimeConsoleHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.*;
import java.util.logging.Handler;

public final class LoggingUtil {

    public static final String LOGTOCONSOLE_PROPERTY = "be.atbash.runtime.logging.handler.LogFileHandler.logtoConsole";
    public static final String LOGTOFILE_PROPERTY = "be.atbash.runtime.logging.handler.LogFileHandler.logToFile";
    public static final String SYSTEM_PROPERTY_LOGGING_CONSOLE = "runtime.logging.console";
    public static final String SYSTEM_PROPERTY_FILE_LOGGING = "runtime.logging.file";
    public static final String SYSTEM_PROPERTY_LOGGING_VERBOSE = "runtime.logging.verbose";

    public static final PrintStream oStdErrBackup = System.err;
    public static final PrintStream oStdOutBackup = System.out;

    private static final String HANDLERS = "handlers";
    private static final String LOGFILEHANDLER = "be.atbash.runtime.logging.handler.LogFileHandler";

    private LoggingUtil() {
    }

    public static void handleConsoleHandlerLogic(Properties loggingProperties) {
        boolean logToConsole = isLogToConsole();

        String handlers = loggingProperties.getProperty(HANDLERS);
        if (logToConsole && !handlers.contains(RuntimeConsoleHandler.class.getName())) {
            loggingProperties.setProperty(HANDLERS, handlers + "," + RuntimeConsoleHandler.class.getName());
        }
        if (!logToConsole && handlers.contains(RuntimeConsoleHandler.class.getName())) {
            loggingProperties.setProperty(HANDLERS, removeHandler(handlers , RuntimeConsoleHandler.class.getName()));
        }

        loggingProperties.put(LOGTOCONSOLE_PROPERTY, Boolean.toString(logToConsole));
    }

    public static void handleLogToFileHandlerLogic(Properties loggingProperties) {
        boolean logToFile = isLogToFile();

        String handlers = loggingProperties.getProperty(HANDLERS);
        if (!logToFile && handlers.contains(LOGFILEHANDLER)) {
            loggingProperties.setProperty(HANDLERS, removeHandler(handlers, LOGFILEHANDLER));
        }

        loggingProperties.put(LOGTOFILE_PROPERTY, Boolean.toString(logToFile));
    }

    private static String removeHandler(String handlers, String handlerName) {
        List<String> parts = new ArrayList<>(Arrays.asList(handlers.split(",")));
        parts.remove(handlerName);
        return String.join(",", parts);
    }

    public static void handleVerboseLogic(Properties loggingProperties) {
        if (isVerbose()) {
            loggingProperties.setProperty("be.atbash.runtime.level", "ALL");
        }
    }

    public static boolean isLogToConsole() {
        return Boolean.parseBoolean(System.getProperty(LoggingUtil.SYSTEM_PROPERTY_LOGGING_CONSOLE, "false"));
    }

    public static boolean isLogToFile() {
        return Boolean.parseBoolean(System.getProperty(LoggingUtil.SYSTEM_PROPERTY_FILE_LOGGING, "true"));
    }

    public static boolean isVerbose() {
        return Boolean.parseBoolean(System.getProperty(LoggingUtil.SYSTEM_PROPERTY_LOGGING_VERBOSE, "false"));
    }

    public static Logger getMainLogger(Class<?> runtimeMainClass) {

        if (!LoggingUtil.isLogToConsole()) {
            // This logger needs access to the console
            java.util.logging.Logger logger = java.util.logging.Logger.getLogger(runtimeMainClass.getName());
            addConsoleHandlerIfNeeded(logger);
        }

        // Now, return the normal SLF4J logger.
        return LoggerFactory.getLogger(runtimeMainClass);
    }

    private static void addConsoleHandlerIfNeeded(java.util.logging.Logger logger) {
        Handler[] originalHandlers = logger.getHandlers();

        Optional<Handler> hasConsoleHandler = Arrays.stream(originalHandlers)
                .filter(h -> h.getClass().equals(RuntimeConsoleHandler.class))
                .findAny();
        if (hasConsoleHandler.isEmpty()) {
            logger.addHandler(new RuntimeConsoleHandler());
        }
    }

}

