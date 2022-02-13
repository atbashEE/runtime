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
package be.atbash.runtime.logging;

import be.atbash.runtime.core.data.AtbashRuntimeConstant;
import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.logging.earlylog.EarlyLogHandler;
import be.atbash.runtime.logging.earlylog.EarlyLogRecords;
import be.atbash.runtime.logging.handler.RuntimeConsoleHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.LogManager;

public final class LoggingManager {
    private static Logger LOGGER;  // Do not initialize as initializeEarlyLogging needs to be run first!!

    private static final LoggingManager INSTANCE = new LoggingManager();

    private EarlyLogHandler handler;

    private LoggingManager() {
    }

    public void initializeEarlyLogging(Boolean logToConsole, Boolean verbose) {
        System.setProperty("java.util.logging.manager", RuntimeLogManager.class.getName());
        System.setProperty(LoggingUtil.SYSTEM_PROPERTY_LOGGING_CONSOLE, logToConsole.toString());
        System.setProperty(LoggingUtil.SYSTEM_PROPERTY_LOGGING_VERBOSE, verbose.toString());

        java.util.logging.Logger rootLogger = getRootLogger();  // This triggers already RuntimeLogManager.readConfiguration.
        Handler[] originalHandlers = rootLogger.getHandlers();
        // remove current handlers = ConsoleHandler
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

    public void removeEarlyLogHandler() {
        LOGGER = LoggerFactory.getLogger(LoggingManager.class);
        java.util.logging.Logger rootLogger = getRootLogger();
        rootLogger.removeHandler(handler);

        java.util.logging.Logger logger = java.util.logging.Logger.getLogger("be.atbash.runtime");
        EarlyLogRecords.getEarlyMessages().forEach(logger::log);
    }

    public void configureLogging(RuntimeConfiguration configuration) {
        LOGGER = LoggerFactory.getLogger(LoggingManager.class);

        // if the system property is already set, we don't need to do anything
        // FIXME Do we keep this from GF/Payara, or do we force the usage of the logging.properties.
        if (System.getProperty(AtbashRuntimeConstant.LOGGING_FILE_SYSTEM_PROPERTY) != null) {
            System.out.println("\n#!## LoggingManager.configureLogging from file" + System.getProperty(AtbashRuntimeConstant.LOGGING_FILE_SYSTEM_PROPERTY));

            return;
        }

        // logging.properties massaging.
        LogManager logMgr = LogManager.getLogManager();
        File loggingPropertiesFile;

        // reset settings
        try {
            if (configuration.isStateless()) {
                loggingPropertiesFile = new File(configuration.getLoggingConfigurationFile());
            } else {
                loggingPropertiesFile = new File(configuration.getConfigDirectory(), "logging.properties");
            }
            System.setProperty(AtbashRuntimeConstant.LOGGING_FILE_SYSTEM_PROPERTY, loggingPropertiesFile.getAbsolutePath());

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

    }


    public static LoggingManager getInstance() {
        return INSTANCE;
    }
}
