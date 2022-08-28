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
package be.atbash.runtime.logging.slf4j.jul;

/*
 * Copyright (c) 2004-2011 QOS.ch
 * All rights reserved.
 *
 * MIT Licensed
 * Original code was in slf4j-jdk14
 */

import be.atbash.runtime.logging.mapping.BundleMapping;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * JULLoggerFactory is an implementation of {@link ILoggerFactory} returning
 * the appropriately named {@link JULLoggerAdapter} instance.
 * <p>
 * Changed for Atbash Runtime
 * <p>
 * - Returns the JDK logger that also has a resourceBundle (if exists) with the name 'msg.<loggername>'.
 *
 * @author Ceki G&uuml;lc&uuml;
 * @author Rudy De Busscher;
 *
 */
public class JULLoggerFactory implements ILoggerFactory {

    /**
     * the root logger is called "" in JUL
     */
    private static final String JUL_ROOT_LOGGER_NAME = "";

    // key: name (String), value: a JULLoggerAdapter;
    private final ConcurrentMap<String, Logger> loggerMap;

    // Based on the Environment Variable 'atbash_log_resourcebundle_warn', should we warn when Resource Bundle is not found.
    private final boolean logResourcebundleWarn;

    private final BundleMapping bundleMapping;

    public JULLoggerFactory() {
        loggerMap = new ConcurrentHashMap<>();
        logResourcebundleWarn = Boolean.parseBoolean(System.getenv("atbash_log_resourcebundle_warn"));
        bundleMapping = BundleMapping.getInstance();
    }

    /*
     * (non-Javadoc)
     * Returns the JDK logger where the ResourceBundle name is the same as the Logger name.
     *
     * @see org.slf4j.ILoggerFactory#getLogger(java.lang.String)
     */
    public Logger getLogger(String name) {
        // the root logger is called "" in JUL
        if (name.equalsIgnoreCase(Logger.ROOT_LOGGER_NAME)) {
            name = JUL_ROOT_LOGGER_NAME;
        }

        Logger slf4jLogger = loggerMap.get(name);
        if (slf4jLogger != null)
            return slf4jLogger;
        else {
            java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(name);
            boolean warnLoggerResourceBundle = false;
            try {
                ResourceBundle bundle = ResourceBundle.getBundle(bundleMapping.defineBundleName(name));
                julLogger.setResourceBundle(bundle);
            } catch (MissingResourceException e) {
                // Ignore
                warnLoggerResourceBundle = logResourcebundleWarn;
            }
            Logger newInstance = new JULLoggerAdapter(julLogger);
            Logger oldInstance = loggerMap.putIfAbsent(name, newInstance);
            Logger result = oldInstance == null ? newInstance : oldInstance;
            if (warnLoggerResourceBundle) {
                result.warn(String.format("LOG-005: Unable to find the ResourceBundle for logger with name '%s'", name));
            }
            return result;
        }
    }
}
