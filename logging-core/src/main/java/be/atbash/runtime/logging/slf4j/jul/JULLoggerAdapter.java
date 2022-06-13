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

import be.atbash.runtime.logging.EnhancedLogRecord;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.*;
import org.slf4j.spi.DefaultLoggingEventBuilder;
import org.slf4j.spi.LocationAwareLogger;

import java.util.logging.Level;

/**
 * A wrapper over {@link java.util.logging.Logger java.util.logging.Logger} in
 * conformity with the {@link Logger} interface. Note that the logging levels
 * mentioned in this class refer to those defined in the java.util.logging
 * package.
 * <p>
 * Changes for Atbash Runtime
 * - Message is passed as is to LogRecord to support ResourceBundles
 * - Usage of the EnhancedLogRecord of Atbash Runtime.
 * - Addition of the MDC
 *
 * @author Ceki G&uuml;lc&uuml;
 * @author Peter Royal
 * @author Rudy De Busscher
 */
public final class JULLoggerAdapter extends LegacyAbstractLogger implements LocationAwareLogger {

    transient final java.util.logging.Logger logger;

    JULLoggerAdapter(java.util.logging.Logger logger) {
        this.logger = logger;
        this.name = logger.getName();
    }

    /**
     * Is this logger instance enabled for the FINEST level?
     *
     * @return True if this Logger is enabled for level FINEST, false otherwise.
     */
    public boolean isTraceEnabled() {
        return logger.isLoggable(Level.FINEST);
    }

    /**
     * Is this logger instance enabled for the FINE level?
     *
     * @return True if this Logger is enabled for level FINE, false otherwise.
     */
    public boolean isDebugEnabled() {
        return logger.isLoggable(Level.FINE);
    }

    /**
     * Is this logger instance enabled for the INFO level?
     *
     * @return True if this Logger is enabled for the INFO level, false otherwise.
     */
    public boolean isInfoEnabled() {
        return logger.isLoggable(Level.INFO);
    }

    /**
     * Is this logger instance enabled for the WARNING level?
     *
     * @return True if this Logger is enabled for the WARNING level, false
     * otherwise.
     */
    public boolean isWarnEnabled() {
        return logger.isLoggable(Level.WARNING);
    }

    /**
     * Is this logger instance enabled for level SEVERE?
     *
     * @return True if this Logger is enabled for level SEVERE, false otherwise.
     */
    public boolean isErrorEnabled() {
        return logger.isLoggable(Level.SEVERE);
    }

    /**
     * Log the message at the specified level with the specified throwable if any.
     * This method creates a LogRecord and fills in caller date before calling this
     * instance's JDK logger.
     */
    @Override
    protected void handleNormalizedLoggingCall(org.slf4j.event.Level level, Marker marker, String msg, Object[] args, Throwable throwable) {
        innerNormalizedLoggingCallHandler(getFullyQualifiedCallerName(), level, marker, msg, args, throwable);
    }

    private void innerNormalizedLoggingCallHandler(String fqcn, org.slf4j.event.Level level, Marker marker, String msg, Object[] args, Throwable throwable) {
        // millis and thread are filled by the constructor
        Level julLevel = slf4jLevelToJULLevel(level);
        EnhancedLogRecord record;
        if (msg != null && msg.contains("{}")) {
            // {} means we have a message using SLF4J style of parameters
            // and thus we need to format the message here
            String formattedMessage = MessageFormatter.basicArrayFormat(msg, args);
            record = new EnhancedLogRecord(julLevel, formattedMessage);
        } else {
            // We can delegate the formatting to the Formatter attached to the logger.
            //Use the appropriate formatter, like {0}
            record = new EnhancedLogRecord(julLevel, msg);
            record.setParameters(args);
            record.setResourceBundle(logger.getResourceBundle());
        }
        record.setLoggerName(getName());
        record.setThrown(throwable);
        fillCallerData(fqcn, record);
        record.captureMDC();
        logger.log(record);
    }

    @Override
    protected String getFullyQualifiedCallerName() {
        return SELF;
    }

    @Override
    public void log(Marker marker, String callerFQCN, int slf4jLevelInt, String message, Object[] arguments, Throwable throwable) {

        org.slf4j.event.Level slf4jLevel = org.slf4j.event.Level.intToLevel(slf4jLevelInt);
        Level julLevel = slf4jLevelIntToJULLevel(slf4jLevelInt);

        if (logger.isLoggable(julLevel)) {
            NormalizedParameters np = NormalizedParameters.normalize(message, arguments, throwable);
            innerNormalizedLoggingCallHandler(callerFQCN, slf4jLevel, marker, np.getMessage(), np.getArguments(), np.getThrowable());
        }
    }

    /**
     * Fill in caller data if possible.
     *
     * @param record The record to update
     */
    private void fillCallerData(String callerFQCN, EnhancedLogRecord record) {
        StackTraceElement[] steArray = new Throwable().getStackTrace();

        int selfIndex = -1;
        for (int i = 0; i < steArray.length; i++) {
            String className = steArray[i].getClassName();

            if (barrierMatch(callerFQCN, className)) {
                selfIndex = i;
                break;
            }
        }

        int found = -1;
        for (int i = selfIndex + 1; i < steArray.length; i++) {
            String className = steArray[i].getClassName();
            if (!(barrierMatch(callerFQCN, className))) {
                found = i;
                break;
            }
        }

        if (found != -1) {
            StackTraceElement ste = steArray[found];
            // setting the class name has the side effect of setting
            // the needToInferCaller variable to false.
            record.setSourceClassName(ste.getClassName());
            record.setSourceMethodName(ste.getMethodName());
        }
    }

    public java.util.logging.Logger getWrappedLogger() {
        return logger;
    }

    static String SELF = JULLoggerAdapter.class.getName();

    static String SUPER = LegacyAbstractLogger.class.getName();
    static String SUPER_OF_SUPER = AbstractLogger.class.getName();
    static String SUBSTITUE = SubstituteLogger.class.getName();
    static String FLUENT = DefaultLoggingEventBuilder.class.getName();

    static String[] BARRIER_CLASSES = new String[]{SUPER_OF_SUPER, SUPER, SELF, SUBSTITUE, FLUENT, "be.atbash.runtime.core.data.watcher.WatcherService"};

    private boolean barrierMatch(String callerFQCN, String candidateClassName) {
        if (candidateClassName.equals(callerFQCN)) {
            return false;
        }
        for (String barrierClassName : BARRIER_CLASSES) {
            if (barrierClassName.equals(candidateClassName)) {
                return true;
            }
        }
        return false;
    }

    private static Level slf4jLevelIntToJULLevel(int levelInt) {
        org.slf4j.event.Level slf4jLevel = org.slf4j.event.Level.intToLevel(levelInt);
        return slf4jLevelToJULLevel(slf4jLevel);
    }

    private static Level slf4jLevelToJULLevel(org.slf4j.event.Level slf4jLevel) {
        Level julLevel;
        switch (slf4jLevel) {
            case TRACE:
                julLevel = Level.FINEST;
                break;
            case DEBUG:
                julLevel = Level.FINE;
                break;
            case INFO:
                julLevel = Level.INFO;
                break;
            case WARN:
                julLevel = Level.WARNING;
                break;
            case ERROR:
                julLevel = Level.SEVERE;
                break;
            default:
                throw new IllegalStateException("Level " + slf4jLevel + " is not recognized.");
        }
        return julLevel;
    }
}
