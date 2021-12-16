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
package be.atbash.runtime.jetty;

import be.atbash.runtime.core.data.RunData;
import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.config.ConfigUtil;
import be.atbash.runtime.core.data.config.Endpoint;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.exception.UnexpectedException;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventPayload;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.core.module.RuntimeObjectsManager;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class JettyModule implements Module<RuntimeConfiguration> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JettyModule.class);

    private RuntimeConfiguration configuration;
    private Server server;
    private HandlerCollection handlers;

    @Override
    public String name() {
        return "jetty";
    }

    @Override
    public String[] dependencies() {
        return new String[0];
    }

    @Override
    public List<Class<?>> getRuntimeObjectTypes() {
        return List.of(HandlerCollection.class);
    }

    @Override
    public <T> T getRuntimeObject(Class<T> exposedObjectType) {
        if (exposedObjectType.equals(HandlerCollection.class)) {
            return (T) handlers;
        }
        return null;
    }

    @Override
    public Specification[] provideSpecifications() {
        return new Specification[]{Specification.HTML, Specification.SERVLET};
    }

    @Override
    public Sniffer moduleSniffer() {
        return new ServletSniffer();
    }

    @Override
    public void onEvent(EventPayload eventPayload) {

    }

    @Override
    public void registerDeployment(ArchiveDeployment deployment) {

        String contextRoot = deployment.getContextRoot();

        WebAppContext handler = new WebAppContext();
        handler.setContextPath(contextRoot);

        handler.setWar(deployment.getDeploymentLocation().getAbsolutePath());
        handler.setParentLoaderPriority(true);  // FIXME Configure

        handlers.addHandler(handler);
        try {
            handler.start();
        } catch (Exception e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }
        // FIXME Print out a list of Servlets. This is not by default possible with Jetty.
        // It is stored here, but not accessible
        // System.out.println(handler.getServletHandler()._servletPathMap);
        // FIXME So we need to get this from the ServletSniffer, including parsing the web.xml
        LOGGER.info("JETTY-104: End of registration of WebApp " + deployment.getDeploymentName());
    }

    public void unregisterDeployment(ArchiveDeployment deployment) {
        // TODO Duplicated between Jetty and Jersey module but we need to keep them independent
        Optional<Handler> handler = Arrays.stream(handlers.getHandlers())
                .filter(h -> h instanceof WebAppContext)
                .filter(wac -> ((WebAppContext) wac).getContextPath().equals(deployment.getContextRoot()))
                .findAny();
        if (handler.isPresent()) {
            try {
                handler.get().stop();
            } catch (Exception e) {
                throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
            }
        }
        LOGGER.info("JETTY-105: Unregistration of WebApp " + deployment.getDeploymentName() + " done");
    }

    @Override
    public void run() {
        Endpoint httpEndpoint = ConfigUtil.getHttpEndpoint(configuration.getConfig());
        server = new Server(httpEndpoint.getPort());
        handlers = new HandlerCollection(true);
        server.setHandler(handlers);
        try {
            server.start();
        } catch (Exception e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }

        LOGGER.info("JETTY:101: Started Jetty");

        if (Arrays.stream(configuration.getRequestedModules()).noneMatch("health"::equals)) {
            // FIXME create a proper health module.
            // But if health module is node active, this HealthHandler should give basics
            // so that MicroShed testing is working.
            // register health
            RunData runData = RuntimeObjectsManager.getInstance().getExposedObject(RunData.class);
            handlers.addHandler(new HealthHandler(runData));
        }
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
        try {
            LOGGER.info("JETTY-105: Shutting down the application...");
            server.stop();
        } catch (Exception e) {
            //Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, e);
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }


    }
}
