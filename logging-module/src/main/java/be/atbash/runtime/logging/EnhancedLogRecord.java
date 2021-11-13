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
package be.atbash.runtime.logging;

import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * This class provides additional attributes not supported by JUL LogRecord

 */
public class EnhancedLogRecord extends LogRecord {

    private String threadName;

    public EnhancedLogRecord(Level level, String msg) {
        super(level, msg);
    }

    public EnhancedLogRecord(LogRecord record) {
        this(record.getLevel(), record.getMessage());

        this.setLoggerName(record.getLoggerName());
        this.setInstant(record.getInstant());
        this.setParameters(record.getParameters());
        this.setResourceBundle(record.getResourceBundle());
        this.setResourceBundleName(record.getResourceBundleName());
        this.setSequenceNumber(record.getSequenceNumber());
        this.setSourceClassName(record.getSourceClassName());
        this.setSourceMethodName(record.getSourceMethodName());
        this.setThreadID(record.getThreadID());
        this.setThrown(record.getThrown());
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    /**
     * wrap log record with {@link EnhancedLogRecord} if not already
     * if setThreadName is true, sets thread name to current
     *
     * @param record
     * @param setThreadName
     * @return wrapped record
     */
    public static EnhancedLogRecord wrap(LogRecord record, boolean setThreadName) {
        EnhancedLogRecord wrappedRecord;
        if (record instanceof EnhancedLogRecord) {
            wrappedRecord = (EnhancedLogRecord)record;
        } else {
            wrappedRecord = new EnhancedLogRecord(record);
        }
        // Check there is actually a set thread name
        if (setThreadName && wrappedRecord.getThreadName() == null) {
            wrappedRecord.setThreadName(Thread.currentThread().getName());
        }

        return wrappedRecord;
    }

}
