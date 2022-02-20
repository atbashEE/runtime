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
package be.atbash.runtime.logging.handler.formatter;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * A Parent Formatter supporting the Exclude Fields Support and providing the logic for the Product version value.
 * Inspired by code of Payara
 */
public abstract class CommonFormatter extends Formatter {

    private final AdditionalLogFieldsSupport additionalLogFieldsSupport;

    protected CommonFormatter(String excludeFields) {
        super();
        this.additionalLogFieldsSupport = new AdditionalLogFieldsSupport(excludeFields);
    }

    protected boolean isFieldIncluded(AdditionalLogFieldsSupport.SupplementalAttribute excludeField) {
        return !additionalLogFieldsSupport.isSet(excludeField);
    }

    protected String formatLogMessage(String logMessage, LogRecord record, Function<String, ResourceBundle> rbGetter) {
        try {
            return formatLogMessage0(logMessage, record.getLoggerName(), record.getParameters(), rbGetter);
        } catch (IllegalArgumentException e) {
            // could not format string objects, try with original objects
            if (record.getParameters() == null || record.getParameters().length < 2
                    // not a multiple of two
                    || (record.getParameters().length % 2) != 0) {
                throw e;
            }
            Object[] parameters = new Object[record.getParameters().length / 2];
            System.arraycopy(record.getParameters(), parameters.length,
                    parameters, 0, parameters.length);
            return formatLogMessage0(logMessage, record.getLoggerName(), parameters, rbGetter);
        }
    }

    private String formatLogMessage0(String logMessage, String loggerName, Object[] parameters,
                                     Function<String, ResourceBundle> rbGetter) {
        if (logMessage.contains("{0") && logMessage.contains("}") && parameters != null) {
            // If we find {0} or {1} etc., in the message, then it's most
            // likely finer level messages for Method Entry, Exit etc.,
            logMessage = MessageFormat.format(logMessage, parameters);
        } else {
            ResourceBundle rb = rbGetter.apply(loggerName);
            if (rb != null && rb.containsKey(logMessage)) {
                try {
                    logMessage = MessageFormat.format(rb.getString(logMessage), parameters);
                } catch (MissingResourceException e) {
                    // If we don't find an entry, then we are covered
                    // because the logMessage is initialized already
                }
            }
        }
        return logMessage;
    }
}
