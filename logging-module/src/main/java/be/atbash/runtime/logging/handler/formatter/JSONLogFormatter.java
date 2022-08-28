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

import be.atbash.json.JSONValue;
import be.atbash.runtime.logging.EnhancedLogRecord;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.*;

/**
 * Class for converting a {@link LogRecord} to Json format.
 * <p>
 * Based on Payara code.
 */

public class JSONLogFormatter extends CommonFormatter {

    private static final String METHOD_NAME = "MethodName";
    private static final String CLASS_NAME = "ClassName";

    private final Map<String, ResourceBundle> loggerResourceBundleTable;
    private final LogManager logManager;

    private final Date date = new Date();

    // Event separator
    private static final String LINE_SEPARATOR = System.lineSeparator();

    // String values for field keys
    private static final String TIMESTAMP_KEY = "Timestamp";
    private static final String LOG_LEVEL_KEY = "Level";
    private static final String LOGGER_NAME_KEY = "LoggerName";
    // String values for exception keys
    private static final String EXCEPTION_KEY = "Exception";
    private static final String STACK_TRACE_KEY = "StackTrace";
    // String values for thread excludable keys
    private static final String THREAD_ID_KEY = "ThreadID";
    private static final String THREAD_NAME_KEY = "ThreadName";
    private static final String LEVEL_VALUE_KEY = "LevelValue";
    private static final String TIME_MILLIS_KEY = "TimeMillis";
    private static final String MESSAGE_ID_KEY = "MessageID";
    private static final String LOG_MESSAGE_KEY = "LogMessage";
    private static final String THROWABLE_KEY = "Throwable";

    private static final String RFC3339_DATE_FORMAT =
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ";


    public JSONLogFormatter(String excludeFields) {
        super(excludeFields);
        loggerResourceBundleTable = new HashMap<>();
        logManager = LogManager.getLogManager();
    }

    @Override
    public String format(LogRecord record) {
        return jsonLogFormat(record);
    }

    @Override
    public String formatMessage(LogRecord record) {
        // TODO Is this correct that we override this?
        return jsonLogFormat(record);
    }

    /**
     * @param record The record to format.
     * @return The JSON formatted record.
     */
    private String jsonLogFormat(LogRecord record) {
        try {
            Map<String, Object> eventObject = new TreeMap<>();

            /*
             * Create the timestamp field and append to object.
             */
            SimpleDateFormat dateFormatter = new SimpleDateFormat(RFC3339_DATE_FORMAT);

            date.setTime(record.getMillis());
            String timestampValue = dateFormatter.format(date);
            eventObject.put(TIMESTAMP_KEY, timestampValue);

            /*
             * Create the event level field and append to object.
             */
            Level eventLevel = record.getLevel();
            eventObject.put(LOG_LEVEL_KEY, eventLevel.getLocalizedName());

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
            if (isFieldIncluded(AdditionalLogFieldsSupport.SupplementalAttribute.TID)) {
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
            if (isFieldIncluded(AdditionalLogFieldsSupport.SupplementalAttribute.TIME_MILLIS)) {
                long timestamp = record.getMillis();
                eventObject.put(TIME_MILLIS_KEY, String.valueOf(timestamp));
            }

            /*
             * Include the integer value for log level
             */
            Level level = record.getLevel();
            if (isFieldIncluded(AdditionalLogFieldsSupport.SupplementalAttribute.LEVEL_VALUE)) {
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
            if (level.intValue() <= Level.FINE.intValue()) {
                String sourceClassName = record.getSourceClassName();

                if (null != sourceClassName && !sourceClassName.isEmpty()) {
                    eventObject.put(CLASS_NAME, sourceClassName);
                }

                String sourceMethodName = record.getSourceMethodName();

                if (null != sourceMethodName && !sourceMethodName.isEmpty()) {
                    eventObject.put(METHOD_NAME, sourceMethodName);
                }
            }

            List<Object> parameters = new ArrayList<>();

            if (record.getParameters() != null) {
                parameters.addAll(Arrays.asList(record.getParameters()));
            }
            if (record instanceof EnhancedLogRecord) {
                // We need to add MDC as a Map since there is specific support for Map parameters.
                parameters.add(((EnhancedLogRecord) record).getMdc());
            }

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

            String logMessage = record.getMessage();

            if (logMessage != null && !logMessage.isBlank()) {
                logMessage = formatLogMessage(logMessage, record, this::getResourceBundle);
                eventObject.put(LOG_MESSAGE_KEY, logMessage);
            }

            Throwable throwable = getThrowable(record);
            if (null != throwable) {
                String stacktraceText = getStacktraceAsText(throwable);
                Map<String, String> traceObject = new TreeMap<>();

                if (throwable.getMessage() != null) {
                    traceObject.put(EXCEPTION_KEY, throwable.getMessage());
                }
                traceObject.put(STACK_TRACE_KEY, stacktraceText);
                eventObject.put(THROWABLE_KEY, traceObject);

            }


            return JSONValue.toJSONString(eventObject) + LINE_SEPARATOR;

        } catch (Exception ex) {
            new ErrorManager().error(
                    "Error in formatting Logrecord", ex,
                    ErrorManager.FORMAT_FAILURE);
            return "";
        }
    }

    private static String getStacktraceAsText(Throwable throwable) throws IOException {
        String logMessage;
        try (StringWriter stringWriter = new StringWriter();
             PrintWriter printWriter = new PrintWriter(stringWriter)) {

            throwable.printStackTrace(printWriter);
            logMessage = stringWriter.toString();
        }
        return logMessage;
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
}
