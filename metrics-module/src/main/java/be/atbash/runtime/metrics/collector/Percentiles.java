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

import java.util.*;
import java.util.stream.Collectors;

public class Percentiles {

    private static final int[] percentiles = {1, 5, 10, 25, 50, 75, 90, 95, 99};

    private final Map<Integer, Long> values = new HashMap<>();

    private final int count;

    public Percentiles(long[] data, int count) {
        // Make a copy so that data doesn't change when calculating.
        int length = Math.min(data.length, count);
        long[] copy = new long[length];
        System.arraycopy(data, 0, copy, 0, length);

        calculatePercentiles(copy);
        this.count = count;
    }

    /**
     * Determines the percentiles of the sorted list of values. Returns 0 when list it empty
     *
     * @param valuesList
     */
    private void calculatePercentiles(List<Long> valuesList) {
        for (int percentile : percentiles) {
            if (valuesList.isEmpty()) {
                values.put(percentile, 0L);
            } else {
                int index = (int) (percentile / 100.0 * valuesList.size()) - 1;
                values.put(percentile, valuesList.get(Math.max(index, 0)));
            }
        }
    }

    /**
     * Determines the percentiles of an unsorted array of values.
     *
     * @param data
     */
    private void calculatePercentiles(long[] data) {
        List<Long> valuesList = Arrays.stream(data)
                .boxed()
                .sorted()
                .collect(Collectors.toList());
        calculatePercentiles(valuesList);
    }

    public int getCount() {
        return count;
    }

    public long getPercentile(PercentileValue percentile) {
        return values.get(percentile.getValue());
    }

    public long getValueP01() {
        return getPercentile(PercentileValue.P0_01);
    }

    public long getValueP05() {
        return getPercentile(PercentileValue.P0_05);
    }

    public long getValueP10() {
        return getPercentile(PercentileValue.P0_10);
    }

    public long getValueP25() {
        return getPercentile(PercentileValue.P0_25);
    }

    public long getValueP50() {
        return getPercentile(PercentileValue.P0_50);
    }

    public long getValueP75() {
        return getPercentile(PercentileValue.P0_75);
    }

    public long getValueP90() {
        return getPercentile(PercentileValue.P0_90);
    }

    public long getValueP95() {
        return getPercentile(PercentileValue.P0_95);
    }

    public long getValueP99() {
        return getPercentile(PercentileValue.P0_99);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Percentiles.class.getSimpleName() + "[", "]")
                .add("valueP01=" + getValueP01())
                .add("valueP05=" + getValueP05())
                .add("valueP10=" + getValueP10())
                .add("valueP25=" + getValueP25())
                .add("valueP50=" + getValueP50())
                .add("valueP75=" + getValueP75())
                .add("valueP90=" + getValueP90())
                .add("valueP95=" + getValueP95())
                .add("valueP99=" + getValueP99())
                .toString();
    }
}