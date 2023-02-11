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
package be.atbash.runtime.metrics.collector;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class SimpleCircularCollectorTest {

    @Test
    void testCircular() {
        SimpleCircularCollector collector = new SimpleCircularCollector(16);
        for (long i = 0; i < 16; i++) {
            collector.handle(i);
        }
        Assertions.assertThat(collector.calculatePercentiles().getValueP50()).isEqualTo(7);
        // replace half of buffer
        for (long i = 16; i < 24; i++) {
            collector.handle(i);
        }
        Assertions.assertThat(collector.calculatePercentiles().getValueP50()).isEqualTo(15);
        // replace second half
        for (long i = 24; i < 32; i++) {
            collector.handle(i);
        }
        Assertions.assertThat(collector.calculatePercentiles().getValueP50()).isEqualTo(23);

    }
}