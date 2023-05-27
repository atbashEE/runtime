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
import be.atbash.runtime.metrics.collector.Percentiles;
import be.atbash.runtime.metrics.jaxrs.RequestMetricsData;
import be.atbash.runtime.metrics.jaxrs.SimplePathSegment;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.UriInfo;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;


@ExtendWith(MockitoExtension.class)
class RequestMetricsPumpTest {

    @Mock
    private UriInfo uriInfoMock;

    private final RequestMetricsPump metricsPump = new RequestMetricsPump();

    @AfterEach
    void cleanup() {
        metricsPump.stop();
    }

    @Test
    void listDeploymentNames() {
        metricsPump.registerApplication(new TestDeployment("JUnit", "/root"));

        List<String> names = metricsPump.listDeploymentNames();

        Assertions.assertThat(names).containsExactly("JUnit");
    }

    @Test
    void listDeploymentNames_doubleRegistration() {
        // Normally we will never have this case, but to be on the safe side we test this.
        metricsPump.registerApplication(new TestDeployment("JUnit", "/root"));
        metricsPump.registerApplication(new TestDeployment("JUnit", "/root"));

        List<String> names = metricsPump.listDeploymentNames();

        Assertions.assertThat(names).containsExactly("JUnit");
    }

    @Test
    void listDeploymentNames_multipleRegistration() {

        metricsPump.registerApplication(new TestDeployment("JUnit1", "/root1"));
        metricsPump.registerApplication(new TestDeployment("JUnit2", "/root2"));

        List<String> names = metricsPump.listDeploymentNames();

        Assertions.assertThat(names).containsExactly("JUnit1", "JUnit2");
    }

    @Test
    void listDeploymentNames_deregistration() {

        metricsPump.registerApplication(new TestDeployment("JUnit1", "/root1"));
        metricsPump.registerApplication(new TestDeployment("JUnit2", "/root2"));
        metricsPump.unregisterApplication(new TestDeployment("JUnit1", "/root1"));

        List<String> names = metricsPump.listDeploymentNames();

        Assertions.assertThat(names).containsExactly("JUnit2");
    }

    @Test
    void listEndpoints() throws InterruptedException {
        metricsPump.registerApplication(new TestDeployment("JUnit", "/root"));

        RequestMetricsData metricsData = defineRequestMetricsData("/root", "/path/to/endpoint", 5);
        metricsPump.offer(metricsData);

        Thread.sleep(5L);  // Wait for the queue to be processed.

        List<String> names = metricsPump.listEndpoints("JUnit");

        Assertions.assertThat(names).containsExactlyInAnyOrder("/*", "GET /path/to/endpoint");
    }

    @Test
    void listEndpoints_multiple() throws InterruptedException {
        metricsPump.registerApplication(new TestDeployment("JUnit", "/root"));

        RequestMetricsData metricsData = defineRequestMetricsData("/root", "/path/to/endpoint", 5);
        metricsPump.offer(metricsData);
        metricsData = defineRequestMetricsData("/root", "/path/to/endpoint2", 7);
        metricsPump.offer(metricsData);

        Thread.sleep(5L);  // Wait for the queue to be processed.

        List<String> names = metricsPump.listEndpoints("JUnit");

        Assertions.assertThat(names).containsExactlyInAnyOrder("/*", "GET /path/to/endpoint", "GET /path/to/endpoint2");
    }

    @Test
    void getEndpointMetrics() throws InterruptedException {
        metricsPump.registerApplication(new TestDeployment("JUnit", "/root"));

        RequestMetricsData metricsData = defineRequestMetricsData("/root", "/path/to/endpoint", 5);
        metricsPump.offer(metricsData);
        metricsData = defineRequestMetricsData("/root", "/path/to/endpoint2", 8);
        metricsPump.offer(metricsData);

        Thread.sleep(5L);  // Wait for the queue to be processed.

        Percentiles percentiles = metricsPump.getEndpointMetrics("JUnit", "GET /path/to/endpoint");

        Assertions.assertThat(percentiles.getValueP01()).isLessThan(8);  // Less than the endpoint2 one, timing is not absolute.
    }

    private RequestMetricsData defineRequestMetricsData(String root, String path, long wait) throws InterruptedException {
        // 'Fixed' URL
        Mockito.when(uriInfoMock.getRequestUri()).thenReturn(URI.create("http://localhost:8080" + root + path));
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        Mockito.when(uriInfoMock.getPathParameters()).thenReturn(map);

        List<PathSegment> segments = Arrays.stream(path.substring(1).split("/"))
                .map(SimplePathSegment::new)
                .collect(Collectors.toList());

        Mockito.when(uriInfoMock.getPathSegments()).thenReturn(segments);

        RequestMetricsData metricsData = new RequestMetricsData(uriInfoMock, "GET");
        Thread.sleep(wait);
        metricsData.stop();

        return metricsData;
    }

    public static class TestDeployment extends AbstractDeployment {

        public TestDeployment(String deploymentName, String contextRoot) {
            super(deploymentName, contextRoot, new HashMap<>());
        }
    }
}