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
// Portions Copyright [2016-2021] [Payara Foundation]

package be.atbash.runtime.logging.handler.formatter;

import be.atbash.runtime.logging.EnhancedLogRecord;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.logging.ErrorManager;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * UniformLogFormatter conforms to the logging format defined by the
 * Log Working Group in Java Webservices Org.
 * The specified format is
 * "[#|DATETIME|LOG_LEVEL|PRODUCT_ID|LOGGER NAME|OPTIONAL KEY VALUE PAIRS|
 * MESSAGE|#]\n"
 *
 * @author Hemanth Puttaswamy
 * <p/>
 *         TODO:
 *         1. Performance improvement. We can Cache the LOG_LEVEL|PRODUCT_ID strings
 *         and minimize the concatenations and revisit for more performance
 *         improvements
 *         2. Need to use Product Name and Version based on the version string
 *         that is part of the product.
 *         3. Stress testing
 *         4. If there is a Map as the last element, need to scan the message to
 *         distinguish key values with the message argument.
 */
public class UniformLogFormatter extends AnsiColorFormatter {

    private static final String RECORD_NUMBER = "RecordNumber";
    private static final String METHOD_NAME = "MethodName";
    private static final String CLASS_NAME = "ClassName";

    // loggerResourceBundleTable caches references to all the ResourceBundle
    // and can be searched using the LoggerName as the key
    private HashMap loggerResourceBundleTable;
    private LogManager logManager;
    // A Dummy Container Date Object is used to format the date
    private Date date = new Date();

    private static boolean LOG_SOURCE_IN_KEY_VALUE = false;

    private static boolean RECORD_NUMBER_IN_KEY_VALUE = false;
    private static final Pattern MESSAGE_ID_PATTERN = Pattern.compile("(\\D+)-(\\d+):\\s(.+)");

    static {
        // FIXME
        String logSource = System.getProperty(
                "com.sun.aas.logging.keyvalue.logsource");
        if ((logSource != null)
                && (logSource.equals("true"))) {
            LOG_SOURCE_IN_KEY_VALUE = true;
        }

        String recordCount = System.getProperty(
                "com.sun.aas.logging.keyvalue.recordnumber");
        if ((recordCount != null)
                && (recordCount.equals("true"))) {
            RECORD_NUMBER_IN_KEY_VALUE = true;
        }
    }

    private long recordNumber = 0;

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private String recordBeginMarker;
    private String recordEndMarker;
    private String recordFieldSeparator;
    private String recordDateFormat;

    private static final String RECORD_BEGIN_MARKER = "[#|";
    private static final String RECORD_END_MARKER = "|#]" + LINE_SEPARATOR;
    private static final char FIELD_SEPARATOR = '|';
    public static final char NVPAIR_SEPARATOR = ';';
    public static final char NV_SEPARATOR = '=';

    private static final String RFC_3339_DATE_FORMAT =
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private boolean multiLineMode;

    private static final String INDENT = "  ";

    public UniformLogFormatter(String excludeFields) {
        super(excludeFields);
        loggerResourceBundleTable = new HashMap();
        logManager = LogManager.getLogManager();
    }

    /**
     * _REVISIT_: Replace the String Array with an HashMap and do some
     * benchmark to determine whether StringCat is faster or Hashlookup for
     * the template is faster.
     */


    @Override
    public String format(LogRecord record) {
        return uniformLogFormat(record);
    }

    @Override
    public String formatMessage(LogRecord record) {
        return uniformLogFormat(record);
    }


    /**
     * Sun One Appserver SE/EE? can override to specify their product specific
     * key value pairs.
     */
    protected void getNameValuePairs(StringBuilder buf, LogRecord record) {

        Object[] parameters = record.getParameters();
        if ((parameters == null) || (parameters.length == 0)) {
            return;
        }

        try {
            for (Object obj : parameters) {
                if (obj == null) {
                    continue;
                }
                if (obj instanceof Map) {
                    for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) obj).entrySet()) {
                        // there are implementations that allow <null> keys...
                        if (entry.getKey() != null) {
                            buf.append(entry.getKey().toString());
                        } else {
                            buf.append("null");
                        }

                        buf.append(NV_SEPARATOR);

                        // also handle <null> values...
                        if (entry.getValue() != null) {
                            buf.append(entry.getValue().toString());
                        } else {
                            buf.append("null");
                        }
                        buf.append(NVPAIR_SEPARATOR);

                    }
                } else if (obj instanceof Collection) {
                    for (Object entry : ((Collection) obj)) {
                        // handle null values (remember the specs)...
                        if (entry != null) {
                            buf.append(entry.toString());
                        } else {
                            buf.append("null");
                        }
                        buf.append(NVPAIR_SEPARATOR);

                    }
//                } else {
//                    buf.append(obj.toString()).append(NVPAIR_SEPARATOR);
                }
            }
        } catch (Exception e) {
            new ErrorManager().error(
                    "Error in extracting Name Value Pairs", e,
                    ErrorManager.FORMAT_FAILURE);
        }
    }

    /**
     * Note: This method is not synchronized, we are assuming that the
     * synchronization will happen at the Log Handler.publish( ) method.
     */
    private String uniformLogFormat(LogRecord record) {

        try {

            SimpleDateFormat dateFormatter = new SimpleDateFormat(getRecordDateFormat() != null ? getRecordDateFormat() : RFC_3339_DATE_FORMAT);

            StringBuilder recordBuffer = new StringBuilder(getRecordBeginMarker() != null ? getRecordBeginMarker() : RECORD_BEGIN_MARKER);
            // The following operations are to format the date and time in a
            // human readable  format.
            // _REVISIT_: Use HiResolution timer to analyze the number of
            // Microseconds spent on formatting date object
            date.setTime(record.getMillis());
            String timestamp = dateFormatter.format(date);
            recordBuffer.append(timestamp);
            if (color()) {
                recordBuffer.append(getColor(record.getLevel()));
            }
            recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);

            recordBuffer.append(record.getLevel().getLocalizedName()).append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);
            if (color()) {
                recordBuffer.append(getReset());
            }

            if (!isFieldExcluded(ExcludeFieldsSupport.SupplementalAttribute.VERSION)) {
                String compId = getProductId();
                recordBuffer.append(compId).append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);
            }

            String loggerName = record.getLoggerName();
            loggerName = (loggerName == null) ? "" : loggerName;
            if (color()) {
                recordBuffer.append(getLoggerColor());
            }
            recordBuffer.append(loggerName).append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);
            if (color()) {
                recordBuffer.append(getReset());
            }
            if (!isFieldExcluded(ExcludeFieldsSupport.SupplementalAttribute.TID)) {
                recordBuffer.append("_ThreadID").append(NV_SEPARATOR);
                recordBuffer.append(record.getThreadID()).append(NVPAIR_SEPARATOR);
                recordBuffer.append("_ThreadName").append(NV_SEPARATOR);
                String threadName;
                if (record instanceof EnhancedLogRecord) {
                    threadName = ((EnhancedLogRecord) record).getThreadName();
                } else {
                    threadName = Thread.currentThread().getName();
                }
                recordBuffer.append(threadName);
                recordBuffer.append(NVPAIR_SEPARATOR);
            }


            // Include the raw long time stamp value in the log
            if (!isFieldExcluded(ExcludeFieldsSupport.SupplementalAttribute.TIME_MILLIS)) {
                recordBuffer.append("_TimeMillis").append(NV_SEPARATOR);
                recordBuffer.append(record.getMillis()).append(NVPAIR_SEPARATOR);
            }

            // Include the integer level value in the log
            Level level = record.getLevel();
            if (!isFieldExcluded(ExcludeFieldsSupport.SupplementalAttribute.LEVEL_VALUE)) {
                recordBuffer.append("_LevelValue").append(NV_SEPARATOR);
                int levelValue = level.intValue();
                recordBuffer.append(levelValue).append(NVPAIR_SEPARATOR);
            }

            String msgId = getMessageId(record);
            if (msgId != null && !msgId.isEmpty()) {
                recordBuffer.append("_MessageID").append(NV_SEPARATOR);
                recordBuffer.append(msgId).append(NVPAIR_SEPARATOR);
            }

            // See 6316018. ClassName and MethodName information should be
            // included for FINER and FINEST log levels.
            if (LOG_SOURCE_IN_KEY_VALUE ||
                    (level.intValue() <= Level.FINE.intValue())) {
                String sourceClassName = record.getSourceClassName();
                // sourceClassName = (sourceClassName == null) ? "" : sourceClassName;
                if (sourceClassName != null && !sourceClassName.isEmpty()) {
                    recordBuffer.append(CLASS_NAME).append(NV_SEPARATOR);
                    recordBuffer.append(sourceClassName);
                    recordBuffer.append(NVPAIR_SEPARATOR);
                }

                String sourceMethodName = record.getSourceMethodName();
                // sourceMethodName = (sourceMethodName == null) ? "" : sourceMethodName;
                if (sourceMethodName != null && !sourceMethodName.isEmpty()) {
                    recordBuffer.append(METHOD_NAME).append(NV_SEPARATOR);
                    recordBuffer.append(sourceMethodName);
                    recordBuffer.append(NVPAIR_SEPARATOR);
                }
            }

            if (RECORD_NUMBER_IN_KEY_VALUE) {
                recordNumber++;
                recordBuffer.append(RECORD_NUMBER).append(NV_SEPARATOR);
                recordBuffer.append(recordNumber).append(NVPAIR_SEPARATOR);
            }

            // Not needed as per the current logging message format. Fixing bug 16849.
            // getNameValuePairs(recordBuffer, record);

            recordBuffer.append(getRecordFieldSeparator() != null ? getRecordFieldSeparator() : FIELD_SEPARATOR);

            if (multiLineMode) {
                recordBuffer.append(LINE_SEPARATOR);
                recordBuffer.append(INDENT);
            }
            String logMessage = record.getMessage();
            // in some case no msg is passed to the logger API. We assume that either:
            // 1. A message was logged in a previous logger call and now just the exception is logged.
            // 2. There is a bug in the calling code causing the message to be missing.
            if (logMessage == null || logMessage.trim().equals("")) {

                if (record.getThrown() != null) {
                    // case 1: Just log the exception instead of a message
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    record.getThrown().printStackTrace(pw);
                    pw.close();
                    logMessage = sw.toString();
                    sw.close();
                } else {
                    // GLASSFISH-18816: Suppress noise.
                    logMessage = "";
                }
                recordBuffer.append(logMessage);
            } else {
                logMessage = formatLogMessage(logMessage, record, this::getResourceBundle);
                StringBuilder logMessageBuffer = new StringBuilder();
                logMessageBuffer.append(logMessage);

                Throwable throwable = getThrowable(record);
                if (throwable != null) {
                    logMessageBuffer.append(LINE_SEPARATOR);
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    throwable.printStackTrace(pw);
                    pw.close();
                    logMessageBuffer.append(sw.toString());
                    sw.close();
                }
                logMessage = logMessageBuffer.toString();
                recordBuffer.append(logMessage);
            }
            recordBuffer.append(getRecordEndMarker() != null ? getRecordEndMarker() : RECORD_END_MARKER).append(LINE_SEPARATOR).append(LINE_SEPARATOR);
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

    public static String formatLogMessage(String logMessage, LogRecord record, Function<String, ResourceBundle> rbGetter) {
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

    private static String formatLogMessage0(String logMessage, String loggerName, Object[] parameters,
                                            Function<String, ResourceBundle> rbGetter) {
        if (logMessage.contains("{0") && logMessage.contains("}") && parameters != null) {
            // If we find {0} or {1} etc., in the message, then it's most
            // likely finer level messages for Method Entry, Exit etc.,
            logMessage = MessageFormat.format(logMessage, parameters);
        } else {
            ResourceBundle rb = rbGetter.apply(loggerName);
            if (rb != null && rb.containsKey(logMessage)) {
                try {
                    logMessage = MessageFormat.format(
                            rb.getString(logMessage), parameters);
                } catch (MissingResourceException e) {
                    // If we don't find an entry, then we are covered
                    // because the logMessage is initialized already
                }
            }
        }
        return logMessage;
    }

    static String getMessageId(LogRecord lr) {
        String msg = lr.getMessage();
        if (msg != null && !msg.isEmpty()) {
            Matcher matcher = MESSAGE_ID_PATTERN.matcher(msg);
            if (matcher.matches()) {
                return matcher.group(1) + "-" + matcher.group(2);
            }
        }
        return null;
    }

    static Throwable getThrowable(LogRecord record) {
        return record.getThrown();
    }

    private synchronized ResourceBundle getResourceBundle(String loggerName) {
        if (loggerName == null) {
            return null;
        }
        ResourceBundle rb = (ResourceBundle) loggerResourceBundleTable.get(
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

    public String getRecordBeginMarker() {
        return recordBeginMarker;
    }

    public void setRecordBeginMarker(String recordBeginMarker) {
        this.recordBeginMarker = recordBeginMarker;
    }

    public String getRecordEndMarker() {
        return recordEndMarker;
    }

    public void setRecordEndMarker(String recordEndMarker) {
        this.recordEndMarker = recordEndMarker;
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

    /**
     * @param value if the multiLineMode has to be set
     */
    public void setMultiLineMode(boolean value) {
        multiLineMode = value;
    }

}
