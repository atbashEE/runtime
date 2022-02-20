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
package be.atbash.runtime.logging.util;

import be.atbash.runtime.logging.handler.LogFileHandler;

import java.util.Optional;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public final class LogUtil {

    private static final Logger LOGGER = Logger.getLogger(LogFileHandler.class.getName());
    private static final String INVALID_PROPERTY = "LOG-010: An invalid value '%s' has been specified for the '%s' attribute in the logging configuration.";
    private static final String LOG_FILE_HANDLER_PREFIX = LogFileHandler.class.getName() + ".";

    private LogUtil() {
    }

    public static String getLogPropertyKey(String name) {
        if (name.contains(".")) {
            return name;
        }
        return LOG_FILE_HANDLER_PREFIX + name;
    }

    public static boolean getBooleanProperty(String name, boolean defaultValue) {
        LogManager manager = LogManager.getLogManager();
        String value = manager.getProperty(name);
        if (value == null) {
            return defaultValue;
        }
        value = value.toLowerCase();
        if (value.equals("true") || value.equals("1")) {
            return true;
        }
        if (value.equals("false") || value.equals("0")) {
            return false;
        }
        return defaultValue;
    }

    public static long getLongProperty(String name, long defaultValue) {
        LogManager manager = LogManager.getLogManager();
        String value = manager.getProperty(name);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            LOGGER.warning(String.format(INVALID_PROPERTY, value, name));
        }
        return defaultValue;

    }

    public static int getIntProperty(String name, int defaultValue) {
        LogManager manager = LogManager.getLogManager();
        String value = manager.getProperty(name);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            LOGGER.warning(String.format(INVALID_PROPERTY, value, name));
        }
        return defaultValue;
    }

    public static Optional<String> getStringProperty(String name) {
        LogManager manager = LogManager.getLogManager();
        return Optional.ofNullable(manager.getProperty(name)).map(String::trim);
    }
}
