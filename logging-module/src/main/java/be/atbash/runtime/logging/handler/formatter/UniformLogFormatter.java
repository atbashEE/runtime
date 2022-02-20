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
// Portions Copyright [2016-2021] [Payara Foundation]

package be.atbash.runtime.logging.handler.formatter;

import be.atbash.runtime.logging.EnhancedLogRecord;
import be.atbash.runtime.logging.util.LogUtil;

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

    private static final String RECORD_BEGIN_MARKER = "[#|";
    private static final String RECORD_END_MARKER = "|#]";
    private static final String RECORD_FIELD_SEPARATOR = "|";
    private static final String RFC_3339_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private static final String METHOD_NAME = "MethodName";
    private static final String CLASS_NAME = "ClassName";

    // loggerResourceBundleTable caches references to all the ResourceBundle
    // and can be searched using the LoggerName as the key
    private final Map<String, ResourceBundle> loggerResourceBundleTable;
    private final LogManager logManager;
    // A Dummy Container Date Object is used to format the date
    private Date date = new Date();


    private static final Pattern MESSAGE_ID_PATTERN = Pattern.compile("(\\D+)-(\\d+):\\s(.+)");

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private String recordBeginMarker;
    private String recordEndMarker;
    private String recordFieldSeparator;
    private String recordDateFormat;

    public static final char NVPAIR_SEPARATOR = ';';
    public static final char NV_SEPARATOR = '=';

    public UniformLogFormatter(String excludeFields) {
        super(excludeFields);

        configure();

        loggerResourceBundleTable = new HashMap<>();
        logManager = LogManager.getLogManager();
    }

    private void configure() {
        recordBeginMarker = LogUtil.getStringProperty(LogUtil.getLogPropertyKey("logFormatBeginMarker")).orElse(RECORD_BEGIN_MARKER);
        recordEndMarker = LogUtil.getStringProperty(LogUtil.getLogPropertyKey("logFormatEndMarker")).orElse(RECORD_END_MARKER);
        recordFieldSeparator = LogUtil.getStringProperty(LogUtil.getLogPropertyKey("logFormatFieldSeparator")).orElse(RECORD_FIELD_SEPARATOR);
        recordDateFormat = LogUtil.getStringProperty(LogUtil.getLogPropertyKey("logFormatDateFormat")).orElse(RFC_3339_DATE_FORMAT);

        SimpleDateFormat sdf = new SimpleDateFormat(recordDateFormat);
        try {
            sdf.format(new Date());
        } catch (Exception e) {
            recordDateFormat = RFC_3339_DATE_FORMAT;
        }
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
     * Note: This method is not synchronized, we are assuming that the
     * synchronization will happen at the Log Handler.publish( ) method.
     */
    private String uniformLogFormat(LogRecord record) {

        try {

            SimpleDateFormat dateFormatter = new SimpleDateFormat(recordDateFormat);

            StringBuilder recordBuffer = new StringBuilder(recordBeginMarker);
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
            recordBuffer.append(recordFieldSeparator);

            recordBuffer.append(record.getLevel().getLocalizedName()).append(recordFieldSeparator);
            if (color()) {
                recordBuffer.append(getReset());
            }

            String loggerName = record.getLoggerName();
            loggerName = (loggerName == null) ? "" : loggerName;
            if (color()) {
                recordBuffer.append(getLoggerColor());
            }
            recordBuffer.append(loggerName).append(recordFieldSeparator);
            if (color()) {
                recordBuffer.append(getReset());
            }
            if (isFieldIncluded(AdditionalLogFieldsSupport.SupplementalAttribute.TID)) {
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
            if (isFieldIncluded(AdditionalLogFieldsSupport.SupplementalAttribute.TIME_MILLIS)) {
                recordBuffer.append("_TimeMillis").append(NV_SEPARATOR);
                recordBuffer.append(record.getMillis()).append(NVPAIR_SEPARATOR);
            }

            // Include the integer level value in the log
            Level level = record.getLevel();
            if (isFieldIncluded(AdditionalLogFieldsSupport.SupplementalAttribute.LEVEL_VALUE)) {
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
            if (level.intValue() <= Level.FINE.intValue()) {
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

            // Not needed as per the current logging message format. Fixing bug 16849.
            // getNameValuePairs(recordBuffer, record);

            recordBuffer.append(recordFieldSeparator);

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
                    logMessageBuffer.append(sw);
                    sw.close();
                }
                logMessage = logMessageBuffer.toString();
                recordBuffer.append(logMessage);
            }
            recordBuffer.append(recordEndMarker).append(LINE_SEPARATOR).append(LINE_SEPARATOR);
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
        ResourceBundle rb = loggerResourceBundleTable.get(loggerName);
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
