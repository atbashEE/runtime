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

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.UriInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestMetricsData {


    private UriInfo requestUriInfo;
    private final String method;

    private final long start;

    private long duration;
    private String fullPath;
    private String path;

    public RequestMetricsData(UriInfo requestUriInfo, String method) {
        this.requestUriInfo = requestUriInfo;
        this.method = method;
        this.start = System.currentTimeMillis();
    }

    public long getDuration() {
        return duration;
    }

    public void stop() {
        duration = System.currentTimeMillis() - start;

        // We also need to extract some info out of UriInfo.

        fullPath = requestUriInfo.getRequestUri().getPath();
        List<PathSegment> segments = requestUriInfo.getPathSegments();
        Map<String, String> pathParametersMapped = calculateParameterMap(requestUriInfo.getPathParameters());

        path = definePath(segments, pathParametersMapped);
        requestUriInfo = null;  // So that RequestContext can be freed.
    }

    private String definePath(List<PathSegment> segments, Map<String, String> pathParametersMapped) {
        StringBuilder result = new StringBuilder();
        for (PathSegment segment : segments) {
            result.append("/");
            result.append(pathParametersMapped.computeIfAbsent(segment.getPath(), k -> segment.getPath()));

        }
        return result.toString();
    }

    private Map<String, String> calculateParameterMap(MultivaluedMap<String, String> pathParameters) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : pathParameters.entrySet()) {
            String pathValue = entry.getValue().get(0);  // TODO How to have multiple entries
            result.put(pathValue, "{" + entry.getKey() + "}");
        }
        return result;
    }

    public String getMethodAndPath() {
        return method + " " + path;
    }

    public String getFullPath() {
        return fullPath;
    }
}
