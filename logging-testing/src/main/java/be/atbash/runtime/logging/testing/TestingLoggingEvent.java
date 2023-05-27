/*
 * Copyright 2021-2023 Rudy De Busscher (https://www.atbash.be)
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
package be.atbash.runtime.logging.testing;

import org.slf4j.helpers.MessageFormatter;

import java.util.ResourceBundle;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * This is not a complete implementation of the {@code LoggingEvent} interface but one that is enough
 * for the retrieval of the message by {code {@code be.atbash.runtime.logging.LoggingUtil#formatMessage(LoggingEvent)}},
 * just as a normal logger would do.
 */
public class TestingLoggingEvent extends LoggingEvent {

    private static final SimpleFormatter formatter = new SimpleFormatter();
    private final LogRecord logRecord;

    public TestingLoggingEvent(LogRecord logRecord) {
        super(logRecord);
        this.logRecord = logRecord;
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        ResourceBundle resourceBundle = getResourceBundle();
        if (resourceBundle != null && resourceBundle.containsKey(message)) {
            message = resourceBundle.getString(message);
        }
        // We need to format the message with arguments
        if (!getArguments().isEmpty()) {
            message = formatMessage(message);
        }
        return message;
    }

    private String formatMessage(String message) {
        String formattedMessage;
        if (message.contains("{}")) {
            // {} means we have a message using SLF4J style of parameters
            // We need to format the message here
            formattedMessage = MessageFormatter.basicArrayFormat(message, getArguments().toArray());

        } else {

            //SimpleFormatter.formatMessage is thread safe and doesn't use class variables so can be used
            // without creating a new instance.
            formattedMessage = formatter.formatMessage(logRecord);
        }
        return formattedMessage;
    }
}