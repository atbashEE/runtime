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

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

class PercentilesTest {

    @Test
    void testConstructorEmptyList() {
        Percentiles percentiles = new Percentiles(new long[0], 0);
        Assertions.assertThat(percentiles.getValueP01()).isEqualTo(0);
        Assertions.assertThat(percentiles.getValueP05()).isEqualTo(0);
        Assertions.assertThat(percentiles.getValueP10()).isEqualTo(0);
        Assertions.assertThat(percentiles.getValueP25()).isEqualTo(0);
        Assertions.assertThat(percentiles.getValueP50()).isEqualTo(0);
        Assertions.assertThat(percentiles.getValueP75()).isEqualTo(0);
        Assertions.assertThat(percentiles.getValueP90()).isEqualTo(0);
        Assertions.assertThat(percentiles.getValueP95()).isEqualTo(0);
        Assertions.assertThat(percentiles.getValueP99()).isEqualTo(0);
    }

    @Test
    void testConstructorSingleValue() {
        long[] data = new long[1];
        data[0] = 123;
        Percentiles percentiles = new Percentiles(data, 1);
        Assertions.assertThat(percentiles.getValueP01()).isEqualTo(123);
        Assertions.assertThat(percentiles.getValueP05()).isEqualTo(123);
        Assertions.assertThat(percentiles.getValueP10()).isEqualTo(123);
        Assertions.assertThat(percentiles.getValueP25()).isEqualTo(123);
        Assertions.assertThat(percentiles.getValueP50()).isEqualTo(123);
        Assertions.assertThat(percentiles.getValueP75()).isEqualTo(123);
        Assertions.assertThat(percentiles.getValueP90()).isEqualTo(123);
        Assertions.assertThat(percentiles.getValueP95()).isEqualTo(123);
        Assertions.assertThat(percentiles.getValueP99()).isEqualTo(123);
    }

    @Test
    void testConstructor() {
        long[] data = new long[100];
        for (int i = 0; i < 100; i++) {
            data[i] = i + 1;
        }
        Percentiles percentiles = new Percentiles(data, 100);
        Assertions.assertThat(percentiles.getValueP01()).isEqualTo(1);
        Assertions.assertThat(percentiles.getValueP05()).isEqualTo(5);
        Assertions.assertThat(percentiles.getValueP10()).isEqualTo(10);
        Assertions.assertThat(percentiles.getValueP25()).isEqualTo(25);
        Assertions.assertThat(percentiles.getValueP50()).isEqualTo(50);
        Assertions.assertThat(percentiles.getValueP75()).isEqualTo(75);
        Assertions.assertThat(percentiles.getValueP90()).isEqualTo(90);
        Assertions.assertThat(percentiles.getValueP95()).isEqualTo(95);
        Assertions.assertThat(percentiles.getValueP99()).isEqualTo(99);
    }

    @Test
    void testConstructorRandom() {
        long[] data = new long[100];
        for (int i = 0; i < 100; i++) {
            data[i] = i + 1;
        }
        shuffle(data);
        Percentiles percentiles = new Percentiles(data, 100);
        Assertions.assertThat(percentiles.getValueP01()).isEqualTo(1);
        Assertions.assertThat(percentiles.getValueP05()).isEqualTo(5);
        Assertions.assertThat(percentiles.getValueP10()).isEqualTo(10);
        Assertions.assertThat(percentiles.getValueP25()).isEqualTo(25);
        Assertions.assertThat(percentiles.getValueP50()).isEqualTo(50);
        Assertions.assertThat(percentiles.getValueP75()).isEqualTo(75);
        Assertions.assertThat(percentiles.getValueP90()).isEqualTo(90);
        Assertions.assertThat(percentiles.getValueP95()).isEqualTo(95);
        Assertions.assertThat(percentiles.getValueP99()).isEqualTo(99);
    }

    private void shuffle(long[] data) {
        // Implementing Fisher–Yates shuffle
        Random rnd = ThreadLocalRandom.current();
        for (int i = data.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            // Simple swap
            long val = data[index];
            data[index] = data[i];
            data[i] = val;
        }
    }
}