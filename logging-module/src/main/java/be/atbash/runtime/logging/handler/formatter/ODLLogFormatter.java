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

import be.atbash.runtime.logging.EnhancedLogRecord;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.ErrorManager;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/**
 * ODLLogFormatter conforms to the logging format defined by the
 * Log Working Group in Oracle.
 * The specified format is
 * "[timestamp] [Message Type/Level] [Message ID] [Logger
 * Name] [Thread ID] [Extra Attributes] Message\n"
 * <p>
 * Adapted from Glassfish
 */
public class ODLLogFormatter extends AnsiColorFormatter {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    // loggerResourceBundleTable caches references to all the ResourceBundle
    // and can be searched using the LoggerName as the key
    private final Map<String, ResourceBundle> loggerResourceBundleTable;

    private final LogManager logManager;


    private static final String FIELD_BEGIN_MARKER = "[";
    private static final String FIELD_END_MARKER = "]";
    private static final char FIELD_SEPARATOR = ' ';

    private static final String RFC_3339_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    public ODLLogFormatter(String excludeFields) {
        super(excludeFields);
        loggerResourceBundleTable = new HashMap<>();
        logManager = LogManager.getLogManager();
    }

    /**
     * _REVISIT_: Replace the String Array with an HashMap and do some
     * benchmark to determine whether StringCat is faster or Hashlookup for
     * the template is faster.
     */

    @Override
    public String format(LogRecord record) {
        return odlLogFormat(record);
    }

    @Override
    public String formatMessage(LogRecord record) {
        return odlLogFormat(record);
    }


    /**
     * Note: This method is not synchronized, we are assuming that the
     * synchronization will happen at the Log Handler.publish( ) method.
     */
    private String odlLogFormat(LogRecord record) {

        try {
            // creating message from log record using resource bundle and appending parameters
            String message = getLogMessage(record);
            if (message == null || message.isEmpty()) {
                return "";
            }

            // Starting formatting message
            // Adding record begin marker
            StringBuilder recordBuffer = new StringBuilder();

            // A Dummy Container Date Object is used to format the date
            Date date = new Date();

            // Adding timestamp
            SimpleDateFormat dateFormatter = new SimpleDateFormat(RFC_3339_DATE_FORMAT);
            date.setTime(record.getMillis());
            recordBuffer.append(FIELD_BEGIN_MARKER);
            String timestamp = dateFormatter.format(date);
            recordBuffer.append(timestamp);
            recordBuffer.append(FIELD_END_MARKER);
            recordBuffer.append(FIELD_SEPARATOR);

            // Adding messageType
            Level logLevel = record.getLevel();
            recordBuffer.append(FIELD_BEGIN_MARKER);
            if (color()) {
                recordBuffer.append(getColor(logLevel));
            }
            String odlLevel = logLevel.getLocalizedName();
            recordBuffer.append(odlLevel);
            if (color()) {
                recordBuffer.append(getReset());
            }
            recordBuffer.append(FIELD_END_MARKER);
            recordBuffer.append(FIELD_SEPARATOR);

            // Adding message ID
            recordBuffer.append(FIELD_BEGIN_MARKER);
            String msgId = UniformLogFormatter.getMessageId(record);
            recordBuffer.append((msgId == null) ? "" : msgId);
            recordBuffer.append(FIELD_END_MARKER);
            recordBuffer.append(FIELD_SEPARATOR);

            // Adding logger Name / module Name
            recordBuffer.append(FIELD_BEGIN_MARKER);
            String loggerName = record.getLoggerName();
            loggerName = (loggerName == null) ? "" : loggerName;
            if (color()) {
                recordBuffer.append(getLoggerColor());
            }
            recordBuffer.append(loggerName);
            if (color()) {
                recordBuffer.append(getReset());
            }
            recordBuffer.append(FIELD_END_MARKER);
            recordBuffer.append(FIELD_SEPARATOR);

            // Adding thread ID
            if (isFieldIncluded(AdditionalLogFieldsSupport.SupplementalAttribute.TID)) {
                recordBuffer.append(FIELD_BEGIN_MARKER);
                recordBuffer.append("tid: _ThreadID=");
                recordBuffer.append(record.getThreadID());
                String threadName;
                if (record instanceof EnhancedLogRecord) {
                    threadName = ((EnhancedLogRecord) record).getThreadName();
                } else {
                    threadName = Thread.currentThread().getName();
                }
                recordBuffer.append(" _ThreadName=");
                recordBuffer.append(threadName);
                recordBuffer.append(FIELD_END_MARKER);
                recordBuffer.append(FIELD_SEPARATOR);
            }

            // Include the raw time stamp
            if (isFieldIncluded(AdditionalLogFieldsSupport.SupplementalAttribute.TIME_MILLIS)) {
                recordBuffer.append(FIELD_BEGIN_MARKER);
                recordBuffer.append("timeMillis: ");
                recordBuffer.append(record.getMillis());
                recordBuffer.append(FIELD_END_MARKER);
                recordBuffer.append(FIELD_SEPARATOR);
            }

            // Include the level value
            if (isFieldIncluded(AdditionalLogFieldsSupport.SupplementalAttribute.LEVEL_VALUE)) {
                recordBuffer.append(FIELD_BEGIN_MARKER);
                recordBuffer.append("levelValue: ");
                recordBuffer.append(logLevel.intValue());
                recordBuffer.append(FIELD_END_MARKER);
                recordBuffer.append(FIELD_SEPARATOR);
            }

            // Adding extra Attributes - class name and method name for FINE and higher level messages
            Level level = record.getLevel();
            if (level.intValue() <= Level.FINE.intValue()) {
                String sourceClassName = record.getSourceClassName();
                if (sourceClassName != null && !sourceClassName.isEmpty()) {
                    recordBuffer.append(FIELD_BEGIN_MARKER);
                    recordBuffer.append("CLASSNAME: ");
                    recordBuffer.append(sourceClassName);
                    recordBuffer.append(FIELD_END_MARKER);
                    recordBuffer.append(FIELD_SEPARATOR);
                }
                String sourceMethodName = record.getSourceMethodName();
                if (sourceMethodName != null && !sourceMethodName.isEmpty()) {
                    recordBuffer.append(FIELD_BEGIN_MARKER);
                    recordBuffer.append("METHODNAME: ");
                    recordBuffer.append(sourceMethodName);
                    recordBuffer.append(FIELD_END_MARKER);
                    recordBuffer.append(FIELD_SEPARATOR);
                }
            }

            if (record instanceof EnhancedLogRecord) {
                EnhancedLogRecord logRecord = (EnhancedLogRecord) record;
                Map<String, String> mdc = logRecord.getMdc();
                if (mdc != null) {
                    for (Map.Entry<String, String> entry : mdc.entrySet()) {
                        recordBuffer.append(FIELD_BEGIN_MARKER);
                        recordBuffer.append(entry.getKey());
                        recordBuffer.append(": ");
                        recordBuffer.append(entry.getValue());
                        recordBuffer.append(FIELD_END_MARKER);
                        recordBuffer.append(FIELD_SEPARATOR);
                    }


                }
            }

            recordBuffer.append(message);
            recordBuffer.append(LINE_SEPARATOR);
            return recordBuffer.toString();
        } catch (Exception ex) {
            new ErrorManager().error(
                    "Error in formatting Logrecord", ex,
                    ErrorManager.FORMAT_FAILURE);
            // We've already notified the exception, the following
            // return is to keep javac happy
            return "";
        }
    }

    private String getLogMessage(LogRecord record) throws IOException {
        String logMessage = record.getMessage();
        if (logMessage == null) {
            logMessage = "";
        }
        logMessage = formatLogMessage(logMessage, record, this::getResourceBundle);
        Throwable throwable = UniformLogFormatter.getThrowable(record);
        if (throwable != null) {
            StringBuilder buffer = new StringBuilder();
            buffer.append(logMessage);
            buffer.append(LINE_SEPARATOR);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            pw.close();
            buffer.append(sw);
            logMessage = buffer.toString();
            sw.close();
        }
        return logMessage;
    }

    private synchronized ResourceBundle getResourceBundle(String loggerName) {
        if (loggerName == null) {
            return null;
        }
        ResourceBundle rb = loggerResourceBundleTable.get(
                loggerName);
        /*
         * Note that logManager.getLogger(loggerName) untrusted code may create loggers with
         * any arbitrary names this method should not be relied on so added code for checking null.
         */
        if (rb == null && logManager.getLogger(loggerName) != null) {
            rb = logManager.getLogger(loggerName).getResourceBundle();
            loggerResourceBundleTable.put(loggerName, rb);
        }
        return rb;
    }


}
