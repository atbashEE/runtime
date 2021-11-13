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
package be.atbash.runtime.logging.handler.formatter;

import be.atbash.runtime.logging.EnhancedLogRecord;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.ErrorManager;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/**
 * ODLLogFormatter conforms to the logging format defined by the
 * Log Working Group in Oracle.
 * The specified format is
 * "[[timestamp] [organization ID] [Message Type/Level] [Message ID] [Logger
 * Name] [Thread ID] [User ID] [ECID] [Extra Attributes] [Message]]\n"
 *
 * @author Naman Mehta
 */
// Removed HK2 service annotations as never used as such, only plain Java instantiation.
public class ODLLogFormatter extends AnsiColorFormatter {

    // loggerResourceBundleTable caches references to all the ResourceBundle
    // and can be searched using the LoggerName as the key
    private Map<String, ResourceBundle> loggerResourceBundleTable;

    private LogManager logManager;

    private static boolean LOG_SOURCE_IN_KEY_VALUE = false;

    private static boolean RECORD_NUMBER_IN_KEY_VALUE = false;

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    static {
        // FIXME
        String logSource = System.getProperty(
                "com.sun.aas.logging.keyvalue.logsource");
        if ((logSource != null) && (logSource.equals("true"))) {
            LOG_SOURCE_IN_KEY_VALUE = true;
        }

        String recordCount = System.getProperty("com.sun.aas.logging.keyvalue.recordnumber");
        if ((recordCount != null) && (recordCount.equals("true"))) {
            RECORD_NUMBER_IN_KEY_VALUE = true;
        }
    }

    private long recordNumber = 0;

    private String recordFieldSeparator;
    private String recordDateFormat;

    private boolean multiLineMode;

    private static final String FIELD_BEGIN_MARKER = "[";
    private static final String FIELD_END_MARKER = "]";

    private static final char FIELD_SEPARATOR = ' ';

    private static final String RFC_3339_DATE_FORMAT =
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private static final String INDENT = "  ";

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
            boolean multiLine = multiLineMode || isMultiLine(message);

            // Starting formatting message
            // Adding record begin marker
            StringBuilder recordBuffer = new StringBuilder();

            // A Dummy Container Date Object is used to format the date
            Date date = new Date();

            // Adding timestamp
            SimpleDateFormat dateFormatter = new SimpleDateFormat(getRecordDateFormat() != null ? getRecordDateFormat() : RFC_3339_DATE_FORMAT);
            date.setTime(record.getMillis());
            recordBuffer.append(FIELD_BEGIN_MARKER);
            String timestamp = dateFormatter.format(date);
            recordBuffer.append(timestamp);
            recordBuffer.append(FIELD_END_MARKER);
            recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);

            // Adding organization ID
            if (!isFieldExcluded(ExcludeFieldsSupport.SupplementalAttribute.VERSION)) {

                recordBuffer.append(FIELD_BEGIN_MARKER);
                String productId = getProductId();
                recordBuffer.append(productId);
                recordBuffer.append(FIELD_END_MARKER);
                recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);
            }

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
            recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);

            // Adding message ID
            recordBuffer.append(FIELD_BEGIN_MARKER);
            String msgId = UniformLogFormatter.getMessageId(record);
            recordBuffer.append((msgId == null) ? "" : msgId);
            recordBuffer.append(FIELD_END_MARKER);
            recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);

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
            recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);

            // Adding thread ID
            if (!isFieldExcluded(ExcludeFieldsSupport.SupplementalAttribute.TID)) {
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
                recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);
            }

            // Include the raw time stamp
            if (!isFieldExcluded(ExcludeFieldsSupport.SupplementalAttribute.TIME_MILLIS)) {
                recordBuffer.append(FIELD_BEGIN_MARKER);
                recordBuffer.append("timeMillis: ");
                recordBuffer.append(record.getMillis());
                recordBuffer.append(FIELD_END_MARKER);
                recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);
            }

            // Include the level value
            if (!isFieldExcluded(ExcludeFieldsSupport.SupplementalAttribute.LEVEL_VALUE)) {
                recordBuffer.append(FIELD_BEGIN_MARKER);
                recordBuffer.append("levelValue: ");
                recordBuffer.append(logLevel.intValue());
                recordBuffer.append(FIELD_END_MARKER);
                recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);
            }

            // Adding extra Attributes - record number
            if (RECORD_NUMBER_IN_KEY_VALUE) {
                recordBuffer.append(FIELD_BEGIN_MARKER);
                recordNumber++;
                recordBuffer.append("RECORDNUMBER: ");
                recordBuffer.append(recordNumber);
                recordBuffer.append(FIELD_END_MARKER);
                recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);
            }

            // Adding extra Attributes - class name and method name for FINE and higher level messages
            Level level = record.getLevel();
            if (LOG_SOURCE_IN_KEY_VALUE ||
                    (level.intValue() <= Level.FINE.intValue())) {
                String sourceClassName = record.getSourceClassName();
                if (sourceClassName != null && !sourceClassName.isEmpty()) {
                    recordBuffer.append(FIELD_BEGIN_MARKER);
                    recordBuffer.append("CLASSNAME: ");
                    recordBuffer.append(sourceClassName);
                    recordBuffer.append(FIELD_END_MARKER);
                    recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);
                }
                String sourceMethodName = record.getSourceMethodName();
                if (sourceMethodName != null && !sourceMethodName.isEmpty()) {
                    recordBuffer.append(FIELD_BEGIN_MARKER);
                    recordBuffer.append("METHODNAME: ");
                    recordBuffer.append(sourceMethodName);
                    recordBuffer.append(FIELD_END_MARKER);
                    recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);
                }
            }

            if (multiLine) {
                recordBuffer.append(FIELD_BEGIN_MARKER).append(FIELD_BEGIN_MARKER);
                recordBuffer.append(LINE_SEPARATOR);
                recordBuffer.append(INDENT);
            }
            recordBuffer.append(message);
            if (multiLine) {
                recordBuffer.append(FIELD_END_MARKER).append(FIELD_END_MARKER);
            }
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

    private boolean isMultiLine(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        String[] lines = message.split(LINE_SEPARATOR);
        return (lines.length > 1);
    }

    private String getLogMessage(LogRecord record) throws IOException {
        String logMessage = record.getMessage();
        if (logMessage == null) {
            logMessage = "";
        }
        logMessage = UniformLogFormatter.formatLogMessage(logMessage, record, this::getResourceBundle);
        Throwable throwable = UniformLogFormatter.getThrowable(record);
        if (throwable != null) {
            StringBuilder buffer = new StringBuilder();
            buffer.append(logMessage);
            buffer.append(LINE_SEPARATOR);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            pw.close();
            buffer.append(sw.toString());
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

    public String getRecordFieldSeparator() {
        return recordFieldSeparator;
    }

    public void setRecordFieldSeparator(String recordFieldSeparator) {
        this.recordFieldSeparator = recordFieldSeparator;
    }

    public String getRecordDateFormat() {
        return recordDateFormat;
    }

    public void setRecordDateFormat(String recordDateFormat) {
        this.recordDateFormat = recordDateFormat;
    }

    public String getMessageWithoutMessageID(String message) {
        String messageID = "";
        if (message.contains(": ")) {
            StringTokenizer st = new StringTokenizer(message, ":");
            messageID = st.nextToken();
            if (messageID.contains(" ")) {
                // here message ID is not proper value so returning original message only
                return message;
            } else {
                // removing message Id and returning message
                message = st.nextToken();
                return message.substring(1, message.length());
            }
        }
        return message;
    }


    /**
     * @param value the multiLineMode to set
     */
    public void setMultiLineMode(boolean value) {
        this.multiLineMode = value;
    }

}
