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

import be.atbash.runtime.core.module.RuntimeObjectsManager;
import be.atbash.runtime.metrics.RequestMetricsPump;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;

import java.io.IOException;
import java.util.List;

@Provider
public class RestMetricsFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final String METRICS_DATA = "ATBASH.METRICS.DATA";
    private final RequestMetricsPump metricsPump;

    public RestMetricsFilter() {
        metricsPump = RuntimeObjectsManager.getInstance().getExposedObject(RequestMetricsPump.class);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        RequestMetricsData metricsData = new RequestMetricsData(requestContext.getUriInfo(), requestContext.getMethod());
        requestContext.setProperty(METRICS_DATA, metricsData);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        RequestMetricsData metricsData = (RequestMetricsData) requestContext.getProperty(METRICS_DATA);
        metricsData.stop();

        metricsPump.offer(metricsData);
    }
}
