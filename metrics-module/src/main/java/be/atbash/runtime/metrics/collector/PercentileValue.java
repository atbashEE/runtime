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

public enum PercentileValue {
    P0_01(1), P0_05(5),P0_10(10),P0_25(25),P0_50(50),P0_75(75),P0_90(90),P0_95(95),P0_99(99);

    private int value;

    PercentileValue(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
