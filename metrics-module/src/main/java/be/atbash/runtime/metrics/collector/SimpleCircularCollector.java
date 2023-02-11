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

public class SimpleCircularCollector implements MetricsCollector {

    private long[] reservoir;
    private int size;

    private int idx;

    private int count;

    public SimpleCircularCollector(int size) {
        this.reservoir = new long[size];
        this.size = size;
    }
    public Percentiles calculatePercentiles() {
        return new Percentiles(reservoir, count);
    }

    @Override
    public void handle(long requestTime) {
        reservoir[idx] = requestTime;
        idx = ++idx % size;
        count++;
    }

    @Override
    public int getCount() {
        return count;
    }
}
