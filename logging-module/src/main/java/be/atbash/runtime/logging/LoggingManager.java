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

import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.logging.earlylog.EarlyLogHandler;
import be.atbash.runtime.logging.earlylog.EarlyLogRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.LogManager;

public final class LoggingManager {
    private static Logger LOGGER;  // Do not initialize as initializeEarlyLogging needs to be run first!!

    private static final LoggingManager INSTANCE = new LoggingManager();
    public static final PrintStream oStdErrBackup = System.err;
    public static final PrintStream oStdOutBackup = System.out;

    private Handler[] originalHandlers;

    private EarlyLogHandler handler;

    private LoggingManager() {
    }

    public void initializeEarlyLogging(Boolean logToConsole) {
        System.setProperty("java.util.logging.manager", RuntimeLogManager.class.getName());
        System.setProperty(LoggingUtil.SYSTEM_PROPERTY_LOGGING_CONSOLE, logToConsole.toString());

        java.util.logging.Logger rootLogger = getRootLogger();
        originalHandlers = rootLogger.getHandlers();
        // remove current handlers.
        Arrays.stream(originalHandlers)
                // RuntimeLogManager.readConfiguration already added
                // our Console Handler which we should not remove.
                .filter(h -> !h.getClass().equals(RuntimeConsoleHandler.class))
                .forEach(rootLogger::removeHandler);

        handler = new EarlyLogHandler();
        rootLogger.addHandler(handler);

    }

    private java.util.logging.Logger getRootLogger() {
        return LogManager.getLogManager().getLogger("");
    }

    /**
     * Add the ConsoleHandler again to the RootLogger and push the EarlyLog to the logger again.
     */
    public void restoreOriginalHandlers() {

        java.util.logging.Logger rootLogger = getRootLogger();
        Arrays.stream(originalHandlers).forEach(rootLogger::addHandler);

        removeEarlyLogHandler();

    }

    public void removeEarlyLogHandler() {
        LOGGER = LoggerFactory.getLogger(LoggingManager.class);
        java.util.logging.Logger rootLogger = getRootLogger();

        rootLogger.removeHandler(handler);
        EarlyLogRecords.getEarlyMessages().forEach(rootLogger::log);
    }

    public void configureLogging(RuntimeConfiguration configuration) {
        LOGGER = LoggerFactory.getLogger(LoggingManager.class);

        // if the system property is already set, we don't need to do anything
        // FIXME Do we keep this from GF/Payara, or do we force the usage of the logging.properties.
        if (System.getProperty("java.util.logging.config.file") != null) {
            System.out.println("\n\n\n#!## LoggingManager.configureLogging : java.util.logging.config.file=" + System.getProperty("java.util.logging.config.file"));

            return;
        }

        // logging.properties massaging.
        final LogManager logMgr = LogManager.getLogManager();
        File loggingPropertiesFile = null;

        // reset settings
        try {

            loggingPropertiesFile = new File(configuration.getConfigDirectory(), "logging.properties");
            System.setProperty("java.util.logging.config.file", loggingPropertiesFile.getAbsolutePath());

            logMgr.readConfiguration();

        } catch (IOException e) {
            // FIXME
            LOGGER.error("Cannot read logging configuration file.", e);
        }


        // force the ConsoleHandler to use GF formatter
        // FIXME

        // finally, listen to changes to the loggingPropertiesFile.properties file
        //listenToChangesOnloggingPropsFile(loggingPropertiesFile, logMgr);
        // FIXME

        // Log the messages that were generated very early before this Service
        // started.  Just use our own logger...

    }

    public static LoggingManager getInstance() {
        return INSTANCE;
    }

    public Logger getMainLogger(Class<?> runtimeMainClass, boolean logToConsole) {

        if (!logToConsole) {
            // The runtime main class needs output to Console!
            java.util.logging.Logger logger = java.util.logging.Logger.getLogger(runtimeMainClass.getName());
            logger.addHandler(new RuntimeConsoleHandler());
        }

        // Now, return the normal SLF4J logger.
        return LoggerFactory.getLogger(runtimeMainClass);
    }
}
