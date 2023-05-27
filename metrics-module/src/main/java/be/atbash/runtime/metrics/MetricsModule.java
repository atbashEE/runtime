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

import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventPayload;
import be.atbash.runtime.core.data.module.event.Events;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.core.module.RuntimeObjectsManager;
import be.atbash.runtime.jersey.JerseyModule;
import be.atbash.runtime.jersey.util.ExtraPackagesUtil;
import be.atbash.runtime.metrics.jetty.AtbashMetricsHandler;
import be.atbash.runtime.metrics.jetty.PrometheusMetricsHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;

import java.util.List;

public class MetricsModule implements Module<RuntimeConfiguration> {

    public static final String METRICS_MODULE_NAME = "metrics";

    private RuntimeConfiguration configuration;

    private HandlerCollection handlers;

    private RequestMetricsPump requestMetricsPump;

    @Override
    public String name() {
        return METRICS_MODULE_NAME;
    }


    @Override
    public String[] dependencies() {
        return new String[]{JerseyModule.JERSEY_MODULE_NAME};
    }

    @Override
    public Specification[] provideSpecifications() {
        return new Specification[0];
    }

    @Override
    public Class<? extends Sniffer> moduleSniffer() {
        return null;
    }

    @Override
    public List<Class<?>> getRuntimeObjectTypes() {
        return List.of(RequestMetricsPump.class);
    }

    @Override
    public <T> T getRuntimeObject(Class<T> exposedObjectType) {
        if (exposedObjectType.equals(RequestMetricsPump.class)) {
            return (T) requestMetricsPump;
        }
        return null;
    }

    @Override
    public void onEvent(EventPayload eventPayload) {
        if (Events.PRE_DEPLOYMENT.equals(eventPayload.getEventCode())) {
            addJAXRSProviders(eventPayload.getPayload());
            requestMetricsPump.registerApplication(eventPayload.getPayload());
        }
        if (Events.UNDEPLOYMENT.equals(eventPayload.getEventCode())) {
            requestMetricsPump.unregisterApplication(eventPayload.getPayload());
        }
    }

    private void addJAXRSProviders(ArchiveDeployment deployment) {

        ExtraPackagesUtil.addPackages(deployment, "be.atbash.runtime.metrics.jaxrs");
    }

    @Override
    public Class<RuntimeConfiguration> getModuleConfigClass() {
        return RuntimeConfiguration.class;
    }

    @Override
    public void setConfig(RuntimeConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void run() {
        requestMetricsPump = new RequestMetricsPump();

        AtbashMetricsHandler atbashMetricsHandler = new AtbashMetricsHandler();
        atbashMetricsHandler.setProvider(requestMetricsPump);

        PrometheusMetricsHandler prometheusMetricsHandler = new PrometheusMetricsHandler();
        prometheusMetricsHandler.setProvider(requestMetricsPump);

        handlers = RuntimeObjectsManager.getInstance().getExposedObject(HandlerCollection.class);
        handlers.addHandler(atbashMetricsHandler);
        handlers.addHandler(prometheusMetricsHandler);

    }

    @Override
    public void stop() {
        requestMetricsPump.stop();
    }
}