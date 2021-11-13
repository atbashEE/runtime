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
package be.atbash.runtime.logging.earlylog;

import be.atbash.runtime.logging.EnhancedLogRecord;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class EarlyLogHandler extends Handler {
    @Override
    public void publish(LogRecord record) {


        EnhancedLogRecord logRecord = EnhancedLogRecord.wrap(record, true);
        // We can't change the message of the LogRecord within the parameter as that would change logRecord for all Handlers
        // So let us use EnhancedLogRecord and change the message on the copy.
        logRecord.setMessage("*" + logRecord.getMessage());
        EarlyLogRecords.add(logRecord);
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {

    }
}
