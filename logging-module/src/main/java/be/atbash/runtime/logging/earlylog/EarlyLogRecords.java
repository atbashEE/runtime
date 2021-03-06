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
package be.atbash.runtime.logging.earlylog;


import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.LogRecord;

/**
 *
 */
public class EarlyLogRecords {
    private final static List<LogRecord> messages =  new CopyOnWriteArrayList<>();

    private EarlyLogRecords() {
        // no instances allowed...
    }

    public static void add(LogRecord logRecord) {
        messages.add(logRecord);

    }

    public static List<LogRecord> getEarlyMessages() {
        return messages;
    }

}
