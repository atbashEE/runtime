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
package be.atbash.runtime;

import be.atbash.json.JSONObject;
import be.atbash.json.parser.JSONParser;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractMapAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.MapAssert;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LogMessageAssert extends AbstractAssert<LogMessageAssert, String> {
    protected LogMessageAssert(String actual) {
        super(actual, LogMessageAssert.class);
    }


    public static LogMessageAssert assertThat(String actual) {
        return new LogMessageAssert(actual);
    }

    public LogMessageAssert isSimpleFormat() {
        isNotNull();
        if (!isSimple()) {
            failWithMessage("Expected log message is not in Simple format");
        }
        return this;

    }

    public LogMessageAssert isUniformFormat() {
        isNotNull();
        if (!isUniform()) {
            failWithMessage("Expected log message is not in Simple format");
        }
        return this;

    }

    public LogMessageAssert isODLFormat() {
        isNotNull();
        if (!isODL()) {
            failWithMessage("Expected log message is not in Simple format");
        }
        return this;

    }

    public LogMessageAssert isJsonFormat() {
        isNotNull();
        if (!isJson()) {
            failWithMessage("Expected log message is not in JSON format");
        }
        return this;
    }

    private boolean isJson() {
        return actual.startsWith("{") && actual.endsWith("}\n");
    }

    private boolean isUniform() {
        return actual.startsWith("[#|") && actual.endsWith("|#]\n");
    }

    private boolean isODL() {
        return actual.startsWith("[") && !actual.endsWith("]\n");
    }

    private boolean isSimple() {
        return !isJson() && !isUniform() && !isODL();
    }

    /**
     * Verifies the timestamp of the message.
     *
     * @param start
     * @param end
     * @return
     */
    public LogMessageAssert hasTimeStamp(ZonedDateTime start, ZonedDateTime end) {
        // Since it is possible that the sec mark is different between the timestamp used by the format
        // message and the one we take (before or after) calling the test method.
        // We check if the log message has a matching timestamp that xas taken before OR after the test method was
        // executed (since it does no take > 1 sec, this always matches one of them)
        isNotNull();
        if (isSimple()) {
            hasTimeStampSimple(start, end);
        }
        if (isJson()) {
            hasTimeStampJSON(start, end);
        }
        if (isUniform()) {
            hasTimeStampUniform(start, end);
        }
        if (isODL()) {
            hasTimeStampODL(start, end);
        }
        return this;
    }

    private void hasTimeStampSimple(ZonedDateTime start, ZonedDateTime end) {
        String defaultTimestampFormat = "%1$tb %1$td, %1$tY %1$tT";
        String startTimeStamp = String.format(defaultTimestampFormat, start);
        String endTimeStamp = String.format(defaultTimestampFormat, end);

        if (!actual.startsWith(startTimeStamp) && !actual.startsWith(endTimeStamp)) {
            failWithMessage("Expected timestamp on log message does not match");
        }
    }

    private void hasTimeStampJSON(ZonedDateTime start, ZonedDateTime end) {

        JSONObject jsonObject = getAsJsonObject();
        if (!jsonObject.containsKey("TimeMillis") || !jsonObject.containsKey("Timestamp")) {
            failWithMessage("The JSON structure is missing the keys 'TimeMillis' and/or 'Timestamp'");
        }

        Object timeMillis = jsonObject.get("TimeMillis");

        long millis = Long.parseLong(timeMillis.toString());
        if (millis < start.toInstant().toEpochMilli() || millis > end.toInstant().toEpochMilli()) {
            failWithMessage("Expected TimeMillis value on log message does not match");
        }

        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        try {
            Date timestamp = dateFormatter.parse(jsonObject.get("Timestamp").toString());

            if (timestamp.getTime() < start.toInstant().toEpochMilli() || timestamp.getTime() > end.toInstant().toEpochMilli()) {
                failWithMessage("Expected TimeMillis value on log message does not match");
            }

        } catch (ParseException e) {
            Assertions.fail(e.getMessage());
        }

    }

    private void hasTimeStampUniform(ZonedDateTime start, ZonedDateTime end) {

        String[] parts = actual.split("\\|");
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        try {
            Date timestamp = dateFormatter.parse(parts[1]);
            if (timestamp.getTime() < start.toInstant().toEpochMilli() || timestamp.getTime() > end.toInstant().toEpochMilli()) {
                failWithMessage("Expected time stamp value on log message does not match");
            }

        } catch (ParseException e) {
            Assertions.fail(e.getMessage());
        }

        String[] subParts = parts[4].split(";");
        for (String subPart : subParts) {
            if (subPart.startsWith("_TimeMillis")) {
                long timeMillis = Long.parseLong(subPart.split("=")[1]);
                if (timeMillis < start.toInstant().toEpochMilli() || timeMillis > end.toInstant().toEpochMilli()) {
                    failWithMessage("Expected TimeMillis value on log message does not match");
                }
            }
        }

    }

    private void hasTimeStampODL(ZonedDateTime start, ZonedDateTime end) {

        String[] parts = actual.split("]");
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        try {
            Date timestamp = dateFormatter.parse(parts[0].substring(1));  // First field but skip first character = [
            if (timestamp.getTime() < start.toInstant().toEpochMilli() || timestamp.getTime() > end.toInstant().toEpochMilli()) {
                failWithMessage("Expected time stamp value on log message does not match");
            }

        } catch (ParseException e) {
            Assertions.fail(e.getMessage());
        }

        Map<String, String> extraFields = retrieveAdditionalPartsAsMap();
        if (extraFields.containsKey("timeMillis")) {
            long timeMillis = Long.parseLong(extraFields.get("timeMillis"));
            if (timeMillis < start.toInstant().toEpochMilli() || timeMillis > end.toInstant().toEpochMilli()) {
                failWithMessage("Expected TimeMillis value on log message does not match");
            }
        }

    }

    /**
     * Verifies the log message, except the time stamp (endsWith)
     *
     * @param expected
     * @return
     */
    public LogMessageAssert hasMessage(String expected) {

        isNotNull();
        if (isUniform()) {
            String[] parts = actual.split("\\|");
            String mainMessage = reassembleMainParts(parts);
            if (!mainMessage.contains(expected)) {
                failWithMessage("Expected log message '%s' does not match the value '%s'", expected, actual);
            }

        }
        if (isODL()) {
            String mainParts = retrieveMainParts();
            if (!mainParts.startsWith(expected)) {
                failWithMessage("Expected log message '%s' does not match the value '%s'", expected, actual);
            }
        }

        if (isSimple()) {
            if (!actual.endsWith(expected)) {
                failWithMessage("Expected log message '%s' does not match the value '%s'", expected, actual);
            }
        }
        if (isJson()) {
            failWithMessage("You can't check the message when it is in JSON format using this method. Use `asMap()` for this purpose.");
        }

        return this;
    }

    private String retrieveMainParts() {
        StringBuilder result = new StringBuilder();
        int previousIdx = -1;
        int lastIdx = -1;
        boolean additionalField = false;
        for (int i = 0; i < actual.length(); i++) {
            if (actual.charAt(i) == '[') {
                previousIdx = i;
            }
            if (actual.charAt(i) == ':') {
                additionalField = true;
            }
            if (actual.charAt(i) == ']') {
                if (!additionalField) {
                    result.append(actual, previousIdx, i + 1);
                    result.append(" ");
                }
                additionalField = false;  // reset
                lastIdx = i;
            }

        }
        if (lastIdx != -1) {
            result.append(actual.substring(lastIdx + 2));
        }
        return result.toString();
    }

    private String reassembleMainParts(String[] parts) {
        String result = parts[1] + "|" +
                parts[2] + "|" +
                parts[3] + "|" +
                parts[5];

        return result;

    }

    public LogMessageAssert hasException(Throwable throwable) {
        isNotNull();
        String[] lines = actual.split("\n");
        if (lines.length < 3) {
            failWithMessage("Expected log message does not has a Stacktrace");
        }
        if (!lines[1].equals(throwable.getClass().getName())) {
            failWithMessage("Expected log message must have Exception class name %s but found %s", throwable.getClass().getName(), lines[1]);
        }
        if (!lines[2].endsWith(throwable.getStackTrace()[0].toString())) {
            failWithMessage("Expected log message must have Stacktrace %s but found %s", throwable.getStackTrace()[0].toString(), lines[2]);
        }
        return this;
    }

    public AbstractMapAssert asMap() {
        isNotNull();
        if (isJson()) {

            JSONObject jsonObject = getAsJsonObject();
            return new MapAssert<>(jsonObject);
        }
        if (isUniform()) {

            String[] parts = actual.split("\\|");
            String[] subParts = parts[4].split(";");
            Map<String, String> result = new HashMap<>();
            for (String subPart : subParts) {
                if (!subPart.isBlank()) {
                    String[] split = subPart.split("=");
                    result.put(split[0], split[1]);
                }
            }
            return new MapAssert<>(result);
        }

        if (isODL()) {
            return new MapAssert<>(retrieveAdditionalPartsAsMap());
        }

        failWithMessage("Not supported for this log format");
        return null;
    }

    private Map<String, String> retrieveAdditionalPartsAsMap() {
        // ODL
        Map<String, String> result = new HashMap<>();
        int previousIdx = -1;
        int colonIdx = -1;
        boolean firstField = true;  // First field is the date having :
        for (int i = 0; i < actual.length(); i++) {
            if (actual.charAt(i) == '[') {
                previousIdx = i;
            }
            if (actual.charAt(i) == ':') {
                colonIdx = i;
            }

            if (actual.charAt(i) == ']') {
                if (colonIdx != -1 && !firstField) {
                    String key = actual.substring(previousIdx + 1, colonIdx);
                    String value = actual.substring(colonIdx + 1, i);
                    result.put(key, value.trim());
                }
                colonIdx = -1;  // reset
                firstField = false;
            }

        }
        return result;
    }

    private JSONObject getAsJsonObject() {
        Object parsed = new JSONParser().parse(actual);
        Assertions.assertThat(parsed).isInstanceOf(JSONObject.class);

        return (JSONObject) parsed;
    }

}
