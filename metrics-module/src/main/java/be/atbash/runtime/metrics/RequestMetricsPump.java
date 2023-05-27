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
package be.atbash.runtime.metrics;

import be.atbash.runtime.core.data.deployment.AbstractDeployment;
import be.atbash.runtime.core.data.exception.UnexpectedException;
import be.atbash.runtime.core.data.util.Synchronizer;
import be.atbash.runtime.metrics.collector.MetricsCollector;
import be.atbash.runtime.metrics.collector.Percentiles;
import be.atbash.runtime.metrics.collector.SimpleCircularCollector;
import be.atbash.runtime.metrics.jaxrs.RequestMetricsData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RequestMetricsPump implements MetricsDataProvider {

    private static final Object LOCK = new Object();

    private Synchronizer synchronizer;

    private final BlockingQueue<RequestMetricsData> pendingMetrics = new ArrayBlockingQueue<>(10000);

    private Thread pump;

    // context -> deploymentName
    private final Map<String, String> activeApplications = new HashMap<>();

    private final Map<EndpointKey, MetricsCollector> collectorsPerEndpoint = new HashMap<>();

    public RequestMetricsPump() {
        initializePump();
    }

    void initializePump() {
        pump = new Thread(() -> {
            synchronizer = new Synchronizer();
            while (!synchronizer.isSignalled()) {
                try {
                    process();
                } catch (Exception e) {
                    // Continue the loop without exiting
                }
            }
            synchronizer.release();
        });
        pump.setName("Metrics data Log pump");
        pump.setDaemon(true);
        pump.start();
    }

    public void stop() {
        if (pump != null) {
            synchronizer.raiseSignal(1, TimeUnit.SECONDS);  // Wait at max 1 sec
        }
    }

    private void process() {
        try {
            RequestMetricsData metricsData = pendingMetrics.take();
            synchronized (LOCK) {
                String application = findApplication(metricsData.getFullPath());
                EndpointKey key = new EndpointKey(application, metricsData.getMethodAndPath());
                MetricsCollector collector = collectorsPerEndpoint.computeIfAbsent(key, this::determineCollector);
                collector.handle(metricsData.getDuration());

                key = new EndpointKey(application, "/*");
                collector = collectorsPerEndpoint.computeIfAbsent(key, this::determineCollector);
                collector.handle(metricsData.getDuration());

            }
        } catch (InterruptedException e) {
            // Keep thread interrupted for correct cleanup and closure.
            Thread.currentThread().interrupt();
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }
    }

    private String findApplication(String path) {
        return activeApplications.entrySet().stream()
                .filter(e -> path.startsWith(e.getKey()))
                .map(Map.Entry::getValue)
                .findAny()
                .orElse("???" + path);
    }

    private MetricsCollector determineCollector(EndpointKey key) {
        // FIXME Make this configurable
        return new SimpleCircularCollector(128);
    }

    public void offer(RequestMetricsData metricsData) {
        if (!pendingMetrics.offer(metricsData)) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, "Queue full");
        }
    }

    public void registerApplication(AbstractDeployment deployment) {
        synchronized (LOCK) {
            activeApplications.put(deployment.getContextRoot(), deployment.getDeploymentName());
        }
    }

    public void unregisterApplication(AbstractDeployment deployment) {
        synchronized (LOCK) {
            activeApplications.remove(deployment.getContextRoot());
            // FIXME clear data for application
        }
    }


    @Override
    public List<String> listDeploymentNames() {
        synchronized (LOCK) {
            return new ArrayList<>(activeApplications.values());
        }
    }

    @Override
    public List<String> listEndpoints(String deploymentName) {
        synchronized (LOCK) {
            return collectorsPerEndpoint.keySet()
                    .stream()
                    .filter(k -> deploymentName.equals(k.getDeploymentName()))
                    .map(EndpointKey::getPath)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public Percentiles getEndpointMetrics(String deploymentName, String endpointPath) {
        synchronized (LOCK) {
            EndpointKey key = new EndpointKey(deploymentName, endpointPath);
            MetricsCollector collector = collectorsPerEndpoint.get(key);
            return collector == null ? null : collector.calculatePercentiles();
        }
    }
}
