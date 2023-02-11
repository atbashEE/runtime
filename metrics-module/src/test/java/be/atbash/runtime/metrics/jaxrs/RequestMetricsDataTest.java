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
package be.atbash.runtime.metrics.jaxrs;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.UriInfo;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class RequestMetricsDataTest {

    @Mock
    private UriInfo uriInfoMock;

    @Test
    void RequestMetricsData_scenario1() throws InterruptedException {
        // 'Fixed' URL
        Mockito.when(uriInfoMock.getRequestUri()).thenReturn(URI.create("http://localhost:8080/root/path/to/endpoint"));
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        Mockito.when(uriInfoMock.getPathParameters()).thenReturn(map);
        List<PathSegment> segments = new ArrayList<>();
        segments.add(new SimplePathSegment("path"));
        segments.add(new SimplePathSegment("to"));
        segments.add(new SimplePathSegment("endpoint"));
        Mockito.when(uriInfoMock.getPathSegments()).thenReturn(segments);

        RequestMetricsData metricsData = new RequestMetricsData(uriInfoMock, "POST");
        Thread.sleep(5L);
        metricsData.stop();

        Assertions.assertThat(metricsData.getDuration()).isGreaterThan(4);
        Assertions.assertThat(metricsData.getFullPath()).isEqualTo("/root/path/to/endpoint");
        Assertions.assertThat(metricsData.getMethodAndPath()).isEqualTo("POST /path/to/endpoint");
    }

    @Test
    void RequestMetricsData_scenario2() throws InterruptedException {
        // 'templated' URL
        Mockito.when(uriInfoMock.getRequestUri()).thenReturn(URI.create("http://localhost:8080/root/hello/Atbash"));
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.add("name", "Atbash");
        Mockito.when(uriInfoMock.getPathParameters()).thenReturn(map);
        List<PathSegment> segments = new ArrayList<>();
        segments.add(new SimplePathSegment("hello"));
        segments.add(new SimplePathSegment("Atbash"));
        Mockito.when(uriInfoMock.getPathSegments()).thenReturn(segments);

        RequestMetricsData metricsData = new RequestMetricsData(uriInfoMock, "GET");
        Thread.sleep(5L);
        metricsData.stop();

        Assertions.assertThat(metricsData.getDuration()).isGreaterThan(4);
        Assertions.assertThat(metricsData.getFullPath()).isEqualTo("/root/hello/Atbash");
        Assertions.assertThat(metricsData.getMethodAndPath()).isEqualTo("GET /hello/{name}");
    }

}