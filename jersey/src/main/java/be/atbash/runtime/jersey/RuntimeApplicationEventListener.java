/*
 * Copyright 2021-2022 Rudy De Busscher (https://www.atbash.be)
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
package be.atbash.runtime.jersey;

import be.atbash.runtime.core.deployment.Deployer;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceModel;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

@Provider
public class RuntimeApplicationEventListener implements ApplicationEventListener {

    // Log messages from this Listener as coming from JerseyModule
    private static final Logger LOGGER = LoggerFactory.getLogger(JerseyModule.class);

    private String applicationPath;

    @Override
    public void onEvent(ApplicationEvent event) {
        if (event.getType() == ApplicationEvent.Type.INITIALIZATION_APP_FINISHED) {
            applicationPath = Deployer.getCurrentDeployment().getDeploymentData(JerseyModuleConstant.APPLICATION_PATH);
            ResourceModel resourceModel = event.getResourceModel();
            final ResourceLogDetails logDetails = new ResourceLogDetails();
            resourceModel.getResources().forEach((resource) -> {
                logDetails.addEndpointLogLines(getLinesFromResource(resource));
            });

            logDetails.log();
        }

    }

    @Override
    public RequestEventListener onRequest(RequestEvent requestEvent) {
        return null;
    }

    private Set<EndpointLogLine> getLinesFromResource(Resource resource) {
        Set<EndpointLogLine> logLines = new HashSet<>();
        populate(this.applicationPath, false, resource, logLines);
        return logLines;
    }

    private void populate(String basePath, Class<?> klass, boolean isLocator,
                          Set<EndpointLogLine> endpointLogLines) {
        populate(basePath, isLocator, Resource.from(klass), endpointLogLines);
    }

    private void populate(String basePath, boolean isLocator, Resource resource,
                          Set<EndpointLogLine> endpointLogLines) {
        if (!isLocator) {
            basePath = normalizePath(basePath, resource.getPath());
        }

        for (ResourceMethod method : resource.getResourceMethods()) {
            if (basePath.contains(".wadl")) {
                continue;
            }
            endpointLogLines.add(new EndpointLogLine(method.getHttpMethod(), basePath, null));
        }

        for (Resource childResource : resource.getChildResources()) {
            for (ResourceMethod method : childResource.getAllMethods()) {
                if (method.getType() == ResourceMethod.JaxrsType.RESOURCE_METHOD) {
                    final String path = normalizePath(basePath, childResource.getPath());
                    if (path.contains(".wadl")) {
                        continue;
                    }
                    endpointLogLines.add(new EndpointLogLine(method.getHttpMethod(), path, null));
                }
            }
        }
    }

    private static String normalizePath(String basePath, String path) {
        if (path == null) {
            return basePath;
        }
        if (basePath.endsWith("/")) {
            return path.startsWith("/") ? basePath + path.substring(1) : basePath + path;
        }
        return path.startsWith("/") ? basePath + path : basePath + "/" + path;
    }

    private static class ResourceLogDetails {

        //private static final Logger logger = LoggerFactory.getLogger(ResourceLogDetails.class);

        private static final Comparator<EndpointLogLine> COMPARATOR
                = Comparator.comparing((EndpointLogLine e) -> e.path)
                .thenComparing((EndpointLogLine e) -> e.httpMethod);

        private final Set<EndpointLogLine> logLines = new TreeSet<>(COMPARATOR);

        private void log() {
            // FIXME Better logging indicating the
            StringBuilder sb = new StringBuilder("All endpoints for Jersey application\n");
            logLines.forEach((line) -> {
                sb.append(line).append("\n");
            });

            LOGGER.info(sb.toString());
        }

        private void addEndpointLogLines(Set<EndpointLogLine> logLines) {
            this.logLines.addAll(logLines);
        }
    }

    private static class EndpointLogLine {

        private static final String DEFAULT_FORMAT = "   %-7s %s";
        final String httpMethod;
        final String path;
        final String format;

        private EndpointLogLine(String httpMethod, String path, String format) {
            this.httpMethod = httpMethod;
            this.path = path;
            this.format = format == null ? DEFAULT_FORMAT : format;
        }

        @Override
        public String toString() {
            return String.format(format, httpMethod, path);
        }
    }
}
