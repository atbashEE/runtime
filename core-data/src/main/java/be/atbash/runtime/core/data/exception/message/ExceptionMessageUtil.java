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
package be.atbash.runtime.core.data.exception.message;

import org.slf4j.helpers.MessageFormatter;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public final class ExceptionMessageUtil {

    private static final ExceptionMessageUtil INSTANCE = new ExceptionMessageUtil();
    private static final SimpleFormatter formatter = new SimpleFormatter();

    private final ExceptionResourceBundle exceptionResourceBundle;

    private ExceptionMessageUtil() {
        exceptionResourceBundle = new ExceptionResourceBundle(Locale.getDefault());

    }

    public static void addModule(String moduleName) {
        INSTANCE.exceptionResourceBundle.addModule(moduleName);
    }

    public static String formatMessage(String key, Object... parameters) {
        String formattedMessage;
        if (key.contains("{}")) {
            // {} means we have a message using SLF4J style of parameters
            // Wand we need to format the message here
            formattedMessage = MessageFormatter.basicArrayFormat(key, parameters);

        } else {
            // We use the Formatter used by the java.util.logger to format the message.
            LogRecord logRecord = new LogRecord(Level.INFO, key);
            logRecord.setParameters(parameters);
            logRecord.setResourceBundle(INSTANCE.exceptionResourceBundle);

            //SimpleFormatter.formatMessage is thread safe and doesn't use class variables so can be used
            // without creating a new instance.
            formattedMessage = formatter.formatMessage(logRecord);
        }

        return formattedMessage;
    }
}
