/*
 * Copyright 2021-2023 Rudy De Busscher (https://www.atbash.be)
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
import be.atbash.runtime.logging.util.LogUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

public class SimpleLogFormatter extends Formatter {

    private final String DEFAULT_FORMAT = "%1$tb %1$td, %1$tY %1$tT %2$s %4$s: %5$s%6$s%n";
    private final String DEFAULT_FORMAT_WITH_MDC = "%1$tb %1$td, %1$tY %1$tT %2$s %4$s: [%7$s]%5$s%6$s%n";

    private final String format;
    private final String formatWithMDC;

    public SimpleLogFormatter() {
        format = LogUtil.getStringProperty(this.getClass().getName() + ".format").orElse(DEFAULT_FORMAT);
        formatWithMDC = LogUtil.getStringProperty(this.getClass().getName() + ".format.mdc").orElse(DEFAULT_FORMAT_WITH_MDC);
    }

    /**
     * Format the given LogRecord.
     * <p>
     * The formatting can be customized by specifying the format string</a>
     * <pre>
     *    {@link String#format String.format}(format, date, source, logger, level, message, thrown);
     * </pre>
     *
     * @param record the log record to be formatted.
     * @return a formatted log record
     */
    @Override
    public String format(LogRecord record) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(record.getInstant(), ZoneId.systemDefault());
        String source;

        if (record.getSourceClassName() != null) {
            source = record.getSourceClassName();
            if (record.getSourceMethodName() != null) {
                source += "#" + record.getSourceMethodName();
            }
        } else {
            source = record.getLoggerName();
        }

        // When message starts with * -> early log but resourceBundle doesn't have the * in front
        boolean earlyLog = record.getMessage() != null && record.getMessage().startsWith("*");
        if (earlyLog) {
            record.setMessage(record.getMessage().substring(1));
        }
        String message = formatMessage(record);
        if (earlyLog) {
            // Put the * back in front.
            message = "*" + message;
        }

        String throwable = "";
        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println();
            record.getThrown().printStackTrace(pw);
            pw.close();
            throwable = sw.toString();
        }

        String mdcValue = null;

        if (record instanceof EnhancedLogRecord) {
            EnhancedLogRecord enhancedLogRecord = (EnhancedLogRecord) record;
            if (enhancedLogRecord.getMdc() != null) {
                mdcValue = enhancedLogRecord.getMdc().entrySet().stream()
                        .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
                        .collect(Collectors.joining(", "));
            }
        }

        if (mdcValue == null) {
            return String.format(format,
                    zdt,
                    source,
                    record.getLoggerName(),
                    record.getLevel().getName(),
                    message,
                    throwable);

        } else {
            return String.format(formatWithMDC,
                    zdt,
                    source,
                    record.getLoggerName(),
                    record.getLevel().getName(),
                    message,
                    throwable,
                    mdcValue);
        }
    }
}
