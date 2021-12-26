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

import be.atbash.json.JSONValue;
import be.atbash.runtime.logging.EnhancedLogRecord;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.*;

/**
 * Class for converting a {@link LogRecord} to Json format
 *
 * @author savage
 * @since 4.1.1.164
 */
// Removed HK2 service annotations as never used as such, only plain Java instantiation.
public class JSONLogFormatter extends CommonFormatter {

    private static final String RECORD_NUMBER = "RecordNumber";
    private static final String METHOD_NAME = "MethodName";
    private static final String CLASS_NAME = "ClassName";

    private Map<String, ResourceBundle> loggerResourceBundleTable;
    private LogManager logManager;

    private final Date date = new Date();

    private static boolean LOG_SOURCE_IN_KEY_VALUE = false;

    private static boolean RECORD_NUMBER_IN_KEY_VALUE = false;

    // Static Initialiser Block
    static {
        String logSource = System.getProperty(
                "com.sun.aas.logging.keyvalue.logsource");
        if ((logSource != null)
                && (logSource.equals("true"))) {
            LOG_SOURCE_IN_KEY_VALUE = true;
        }

        String recordCount = System.getProperty("com.sun.aas.logging.keyvalue.recordnumber");
        if ((recordCount != null)
                && (recordCount.equals("true"))) {
            RECORD_NUMBER_IN_KEY_VALUE = true;
        }
    }

    private long recordNumber = 0;
    private String recordDateFormat;

    // Event separator
    private static final String LINE_SEPARATOR = System.lineSeparator();

    // String values for field keys
    private String TIMESTAMP_KEY = "Timestamp";
    private String LOG_LEVEL_KEY = "Level";
    private String PRODUCT_ID_KEY = "Version";
    private String LOGGER_NAME_KEY = "LoggerName";
    // String values for exception keys
    private String EXCEPTION_KEY = "Exception";
    private String STACK_TRACE_KEY = "StackTrace";
    // String values for thread excludable keys
    private String THREAD_ID_KEY = "ThreadID";
    private String THREAD_NAME_KEY = "ThreadName";
    private String LEVEL_VALUE_KEY = "LevelValue";
    private String TIME_MILLIS_KEY = "TimeMillis";
    private String MESSAGE_ID_KEY = "MessageID";
    private String LOG_MESSAGE_KEY = "LogMessage";
    private String THROWABLE_KEY = "Throwable";

    private static final String RFC3339_DATE_FORMAT =
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    /**
     * For backwards compatibility with log format for pre-182
     *
     * @deprecated
     */
    @Deprecated
    private static final String PAYARA_JSONLOGFORMATTER_UNDERSCORE = "fish.payara.deprecated.jsonlogformatter.underscoreprefix";
    // FIXME remove

    public JSONLogFormatter(String excludeFields) {
        super(excludeFields);
        loggerResourceBundleTable = new HashMap<>();
        logManager = LogManager.getLogManager();

        String underscorePrefix = logManager.getProperty(PAYARA_JSONLOGFORMATTER_UNDERSCORE);
        if (Boolean.parseBoolean(underscorePrefix)) {
            TIMESTAMP_KEY = "_" + TIMESTAMP_KEY;
            LOG_LEVEL_KEY = "_" + LOG_LEVEL_KEY;
            PRODUCT_ID_KEY = "_" + PRODUCT_ID_KEY;
            LOGGER_NAME_KEY = "_" + LOGGER_NAME_KEY;
            EXCEPTION_KEY = "_" + EXCEPTION_KEY;
            STACK_TRACE_KEY = "_" + STACK_TRACE_KEY;
            // String values for thread excludable keys
            THREAD_ID_KEY = "_" + THREAD_ID_KEY;
            THREAD_NAME_KEY = "_" + THREAD_NAME_KEY;
            LEVEL_VALUE_KEY = "_" + LEVEL_VALUE_KEY;
            TIME_MILLIS_KEY = "_" + TIME_MILLIS_KEY;
            MESSAGE_ID_KEY = "_" + MESSAGE_ID_KEY;
            LOG_MESSAGE_KEY = "_" + LOG_MESSAGE_KEY;
            THROWABLE_KEY = "_" + THROWABLE_KEY;
        }
    }

    @Override
    public String format(LogRecord record) {
        return jsonLogFormat(record);
    }

    @Override
    public String formatMessage(LogRecord record) {
        return jsonLogFormat(record);
    }

    /**
     * @param record The record to format.
     * @return The JSON formatted record.
     */
    private String jsonLogFormat(LogRecord record) {
        try {
            Map<String, String> eventObject = new TreeMap<>();

            /*
             * Create the timestamp field and append to object.
             */
            SimpleDateFormat dateFormatter;

            if (null != getRecordDateFormat()) {
                dateFormatter = new SimpleDateFormat(getRecordDateFormat());
            } else {
                dateFormatter = new SimpleDateFormat(RFC3339_DATE_FORMAT);
            }

            date.setTime(record.getMillis());
            String timestampValue = dateFormatter.format(date);
            eventObject.put(TIMESTAMP_KEY, timestampValue);

            /*
             * Create the event level field and append to object.
             */
            Level eventLevel = record.getLevel();
            eventObject.put(LOG_LEVEL_KEY, eventLevel.getLocalizedName());

            /*
             * Get the product id and append to object.
             */
            if (!isFieldExcluded(ExcludeFieldsSupport
                    .SupplementalAttribute.VERSION)) {

                String productId = getProductId();
                eventObject.put(PRODUCT_ID_KEY, productId);
            }
            /*
             * Get the logger name and append to object.
             */
            String loggerName = record.getLoggerName();

            if (null == loggerName) {
                loggerName = "";
            }

            eventObject.put(LOGGER_NAME_KEY, loggerName);

            /*
             * Get thread information and append to object if not excluded.
             */
            if (!isFieldExcluded(ExcludeFieldsSupport
                    .SupplementalAttribute.TID)) {
                // Thread ID
                int threadId = record.getThreadID();
                eventObject.put(THREAD_ID_KEY, String.valueOf(threadId));

                // Thread Name
                String threadName;

                if (record instanceof EnhancedLogRecord) {
                    threadName = ((EnhancedLogRecord) record).getThreadName();
                } else {
                    threadName = Thread.currentThread().getName();
                }

                eventObject.put(THREAD_NAME_KEY, threadName);
            }


            /*
             * Get millis time for log entry timestamp
             */
            if (!isFieldExcluded(ExcludeFieldsSupport
                    .SupplementalAttribute.TIME_MILLIS)) {
                long timestamp = record.getMillis();
                eventObject.put(TIME_MILLIS_KEY, String.valueOf(timestamp));
            }

            /*
             * Include the integer value for log level
             */
            Level level = record.getLevel();
            if (!isFieldExcluded(ExcludeFieldsSupport
                    .SupplementalAttribute.LEVEL_VALUE)) {
                int levelValue = level.intValue();
                eventObject.put(LEVEL_VALUE_KEY, String.valueOf(levelValue));
            }

            /*
             * Stick the message id on the entry
             */
            String messageId = getMessageId(record);
            if (messageId != null && !messageId.isEmpty()) {
                eventObject.put(MESSAGE_ID_KEY, messageId);
            }

            /*
             * Include ClassName and MethodName for FINER and FINEST log levels.
             */
            if (LOG_SOURCE_IN_KEY_VALUE ||
                    level.intValue() <= Level.FINE.intValue()) {
                String sourceClassName = record.getSourceClassName();

                if (null != sourceClassName && !sourceClassName.isEmpty()) {
                    eventObject.put(CLASS_NAME, sourceClassName);
                }

                String sourceMethodName = record.getSourceMethodName();

                if (null != sourceMethodName && !sourceMethodName.isEmpty()) {
                    eventObject.put(METHOD_NAME, sourceMethodName);
                }
            }

            /*
             * Add the record number to the entry.
             */
            if (RECORD_NUMBER_IN_KEY_VALUE) {
                recordNumber++;
                eventObject.put(RECORD_NUMBER, String.valueOf(recordNumber));
            }

            Object[] parameters = record.getParameters();
            if (parameters != null) {
                for (Object parameter : parameters) {
                    if (parameter instanceof Map) {
                        for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) parameter).entrySet()) {
                            // there are implementations that allow <null> keys...
                            String key;
                            if (entry.getKey() != null) {
                                key = entry.getKey().toString();
                            } else {
                                key = "null";
                            }

                            // also handle <null> values...
                            if (entry.getValue() != null) {
                                eventObject.put(key, entry.getValue().toString());
                            } else {
                                eventObject.put(key, "null");
                            }
                        }
                    }
                }
            }

            String logMessage = record.getMessage();

            if (null == logMessage || logMessage.trim().equals("")) {
                Throwable throwable = record.getThrown();
                if (null != throwable) {
                    Map<String, String> traceObject = new TreeMap<>();

                    try (StringWriter stringWriter = new StringWriter();
                         PrintWriter printWriter = new PrintWriter(stringWriter)) {
                        throwable.printStackTrace(printWriter);
                        if (throwable.getMessage() != null) {
                            traceObject.put(EXCEPTION_KEY, throwable.getMessage());
                        }
                        logMessage = stringWriter.toString();

                        traceObject.put(STACK_TRACE_KEY, logMessage);
                        eventObject.put(THROWABLE_KEY, JSONValue.toJSONString(traceObject));
                    }
                }
            } else {
                logMessage = UniformLogFormatter.formatLogMessage(logMessage, record, this::getResourceBundle);
                StringBuilder logMessageBuilder = new StringBuilder();
                logMessageBuilder.append(logMessage);

                Throwable throwable = getThrowable(record);
                if (null != throwable) {
                    try (StringWriter stringWriter = new StringWriter();
                         PrintWriter printWriter = new PrintWriter(stringWriter)) {

                        Map<String, String> traceObject = new TreeMap<>();

                        throwable.printStackTrace(printWriter);
                        logMessage = stringWriter.toString();
                        traceObject.put(EXCEPTION_KEY, logMessageBuilder.toString());
                        traceObject.put(STACK_TRACE_KEY, logMessage);
                        eventObject.put(THROWABLE_KEY, JSONValue.toJSONString(traceObject));

                    }
                } else {
                    logMessage = logMessageBuilder.toString();
                    eventObject.put(LOG_MESSAGE_KEY, logMessage);
                }
            }

            return JSONValue.toJSONString(eventObject) + LINE_SEPARATOR;

        } catch (Exception ex) {
            new ErrorManager().error(
                    "Error in formatting Logrecord", ex,
                    ErrorManager.FORMAT_FAILURE);
            return "";
        }
    }

    /**
     * @param record
     * @return
     */
    static String getMessageId(LogRecord record) {
        String message = record.getMessage();
        if (null != message && !message.isEmpty()) {
            ResourceBundle bundle = record.getResourceBundle();
            if (null != bundle && bundle.containsKey(message)) {
                if (!bundle.getString(message).isEmpty()) {
                    return message;
                }
            }
        }
        return null;
    }

    /**
     * @param record
     * @return
     */
    static Throwable getThrowable(LogRecord record) {
        return record.getThrown();
    }

    /**
     * @param loggerName Name of logger to get the ResourceBundle of.
     * @return The ResourceBundle for the logger name given.
     */
    private synchronized ResourceBundle getResourceBundle(String loggerName) {
        if (loggerName == null) {
            return null;
        }

        ResourceBundle bundle = loggerResourceBundleTable.get(loggerName);

        /*
         *  logManager.getLogger should not be relied upon.
         *  To deal with this check if bundle is null and logger is not.
         *  Put a new logger and bundle in the resource bundle table if so.
         */
        Logger logger = logManager.getLogger(loggerName);
        if (null == bundle && null != logger) {
            bundle = logger.getResourceBundle();
            loggerResourceBundleTable.put(loggerName, bundle);
        }

        return bundle;
    }

    /**
     * @return The date format for the record.
     */
    public String getRecordDateFormat() {
        return recordDateFormat;
    }

    /**
     * @param recordDateFormat The date format to set for records.
     */
    public void setRecordDateFormat(String recordDateFormat) {
        this.recordDateFormat = recordDateFormat;
    }

}
