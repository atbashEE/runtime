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
package be.atbash.runtime.remotecli.util;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public final class TimeUtil {

    private TimeUtil() {
    }

    public static String getTimeDescription(Long seconds) {
        String result;
        Duration duration = Duration.ofSeconds(seconds);
        if (duration.toDays() > 1) {

            long days = duration.toDays();

            result = days + "d " + formatDuration(duration.minus(days, ChronoUnit.DAYS));
            // remove seconds when we have days
            int idx = result.lastIndexOf(" ");
            result = result.substring(0, idx);
        } else {
            result = formatDuration(duration);
        }
        return result;
    }

    private static String formatDuration(Duration duration) {
        return duration.toString()
                .substring(2)
                .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                .toLowerCase();
    }
}
