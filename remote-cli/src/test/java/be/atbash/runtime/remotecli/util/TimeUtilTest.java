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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimeUtilTest {

    @Test
    void getTimeDescription() {
        assertThat(TimeUtil.getTimeDescription(2L)).isEqualTo("2s");
    }

    @Test
    void getTimeDescription_minutes() {
        assertThat(TimeUtil.getTimeDescription(65L)).isEqualTo("1m 5s");
    }

    @Test
    void getTimeDescription_hours() {
        assertThat(TimeUtil.getTimeDescription(7398L)).isEqualTo("2h 3m 18s");
    }

    @Test
    void getTimeDescription_day() {
        assertThat(TimeUtil.getTimeDescription(3600 * 24 + 12345L)).isEqualTo("27h 25m 45s");
    }

    @Test
    void getTimeDescription_days() {
        assertThat(TimeUtil.getTimeDescription(2 * 3600 * 24 + 54321L)).isEqualTo("2d 15h 5m");
    }

    @Test
    void getTimeDescription_long() {
        assertThat(TimeUtil.getTimeDescription(16133712L)).isEqualTo("186d 17h 35m");
    }
}