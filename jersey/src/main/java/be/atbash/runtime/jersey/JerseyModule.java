/*
 * Copyright 2021 Rudy De Busscher (https://www.atbash.be)
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

import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventPayload;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.core.module.ExposedObjectsModuleManager;
import be.atbash.runtime.jersey.util.ResourcePathUtil;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class JerseyModule implements Module<RuntimeConfiguration> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JerseyModule.class);

    private RuntimeConfiguration configuration;
    private HandlerCollection handlers;

    @Override
    public String name() {
        return "jersey";
    }

    @Override
    public String[] dependencies() {
        return new String[]{"jetty"};
    }

    @Override
    public List<Class<?>> getExposedTypes() {
        return Collections.emptyList();
    }

    @Override
    public <T> T getExposedObject(Class<T> exposedObjectType) {
        return null;
    }

    @Override
    public Specification[] provideSpecifications() {
        return new Specification[]{Specification.REST};
    }

    @Override
    public Sniffer moduleSniffer() {
        return new RestSniffer();
    }

    @Override
    public void onEvent(EventPayload eventPayload) {

    }

    @Override
    public void registerDeployment(ArchiveDeployment deployment) {
        WebAppContext handler = new WebAppContext();

        String contextRoot = deployment.getContextRoot();

        RestSniffer restSniffer = getRestSniffer(deployment);
        handler.setContextPath(contextRoot);

        handler.setWar(deployment.getDeploymentLocation().getAbsolutePath());
        handler.setParentLoaderPriority(true);  // FIXME Configure

        String applicationPath = ResourcePathUtil.getInstance().findApplicationPath(deployment);

        ServletHolder jerseyServlet = handler.addServlet(
                org.glassfish.jersey.servlet.ServletContainer.class, applicationPath + "/*");

        List<String> resourcePackages = ResourcePathUtil.getInstance().determinePackages(restSniffer);
        resourcePackages.add("be.atbash.runtime.jersey");
        String packages = String.join(";", resourcePackages);

        jerseyServlet.setInitParameter(
                "jersey.config.server.provider.packages",
                packages);

        jerseyServlet.setInitOrder(0);


        handlers.addHandler(handler);
        try {
            handler.start();
        } catch (Exception e) {
            e.printStackTrace();
            // FIXME
        }

        LOGGER.info("JERSEY-104: End of registration of WebApp " + deployment.getDeploymentName());
    }

    private RestSniffer getRestSniffer(ArchiveDeployment deployment) {
        return (RestSniffer) deployment.getSniffers().stream()
                .filter(sn -> sn.getClass().equals(RestSniffer.class))
                .findAny().get();  // Guaranteed to be found
    }

    @Override
    public void run() {

        handlers = ExposedObjectsModuleManager.getInstance().getExposedObject(HandlerCollection.class);

        LOGGER.info("JERSEY-101: Started Jersey");

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
    public void stop() {


    }
}
