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
package be.atbash.runtime.metrics.jetty;

import be.atbash.runtime.metrics.MetricsDataProvider;
import be.atbash.runtime.metrics.collector.Percentiles;

import java.util.*;

public class TestDataProvider implements MetricsDataProvider {

    private final Set<String> deployments = new HashSet<>();

    private final Map<String, List<String>> allEndpoints = new HashMap<>();

    private final Map<String, Percentiles> allPercentiles = new HashMap<>();

    public void addData(String name, String endpoint, Percentiles percentiles) {
        deployments.add(name);
        List<String> endpoints = allEndpoints.computeIfAbsent(name, (k) -> new ArrayList<>());
        endpoints.add(endpoint);
        allPercentiles.put(getKey(name, endpoint), percentiles);
    }

    private static String getKey(String name, String endpoint) {
        return name + "-" + endpoint;
    }

    @Override
    public List<String> listDeploymentNames() {
        ArrayList<String> result = new ArrayList<>(deployments);
        Collections.sort(result);
        return result;
    }

    @Override
    public List<String> listEndpoints(String deploymentName) {
        return allEndpoints.get(deploymentName);
    }

    @Override
    public Percentiles getEndpointMetrics(String deploymentName, String endpointPath) {
        return allPercentiles.get(getKey(deploymentName, endpointPath));
    }
}
