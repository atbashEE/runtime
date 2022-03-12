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
package be.atbash.runtime.jetty;

import be.atbash.runtime.core.data.RunData;
import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.config.ConfigHelper;
import be.atbash.runtime.core.data.config.Endpoint;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.exception.UnexpectedException;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventPayload;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.core.data.watcher.WatcherService;
import be.atbash.runtime.core.module.RuntimeObjectsManager;
import be.atbash.runtime.logging.LoggingUtil;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletHandler;
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
        // If an exception happens during deployment (like CDI issue) make sure we can handle the exception
        //and deployment can be handled as failed from our code.
        handler.setThrowUnavailableOnStartupException(true);

        handler.setContextPath(contextRoot);

        handler.setWar(deployment.getDeploymentLocation().getAbsolutePath());
        handler.setParentLoaderPriority(true);  // FIXME Configure

        handlers.addHandler(handler);
        try {
            handler.start();
        } catch (Exception e) {
            deployment.setDeploymentException(e);
            return;
        }

        if (LoggingUtil.isVerbose()) {
            printServlets(handler, deployment.getDeploymentName());
        }
        LOGGER.info("JETTY-104: End of registration of WebApp " + deployment.getDeploymentName());
    }

    private void printServlets(WebAppContext webAppContext, String deploymentName) {
        ServletHandler servletHandler = webAppContext.getServletHandler();
        StringBuilder servlets = new StringBuilder();
        Arrays.stream(servletHandler.getServletMappings())
                .filter(mapping -> !mapping.getServletName().equals("jsp")) // TODO When we add support for JSPs add it back.
                .forEach(mapping -> servlets.append(
                        String.format("%s\t=>\t%s%n",
                                String.join(",", mapping.getPathSpecs()),
                                mapping.getServletName())));

        LOGGER.trace(String.format("All endpoints for Web application '%s'\n%s", deploymentName, servlets));
    }

    public void unregisterDeployment(ArchiveDeployment deployment) {
        // TODO Duplicated between Jetty and Jersey module but we need to keep them independent
        Optional<Handler> handler = Arrays.stream(handlers.getHandlers())
                .filter(h -> h instanceof WebAppContext)
                .filter(wac -> ((WebAppContext) wac).getContextPath().equals(deployment.getContextRoot()))
                .findAny();
        if (handler.isPresent()) {
            try {
                Handler webAppContextHandler = handler.get();
                webAppContextHandler.stop();
                handlers.removeHandler(webAppContextHandler);
            } catch (Exception e) {
                throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
            }
        }
        LOGGER.info("JETTY-105: Unregistration of WebApp " + deployment.getDeploymentName() + " done");
    }

    @Override
    public void run() {
        WatcherService watcherService = RuntimeObjectsManager.getInstance().getExposedObject(WatcherService.class);
        watcherService.logWatcherEvent("Jetty", "JETTY-1001: Module startup", false);

        Endpoint httpEndpoint = ConfigHelper.getHttpEndpoint(configuration.getConfig());
        server = new Server(httpEndpoint.getPort());
        handlers = new HandlerCollection(true);
        server.setHandler(handlers);
        try {
            server.start();
        } catch (Exception e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }

        if (Arrays.stream(configuration.getRequestedModules()).noneMatch("health"::equals)) {
            // FIXME create a proper health module.
            // But if health module is node active, this HealthHandler should give basics
            // so that MicroShed testing is working.
            // register health
            RunData runData = RuntimeObjectsManager.getInstance().getExposedObject(RunData.class);
            handlers.addHandler(new HealthHandler(runData));
        }

        watcherService.logWatcherEvent("Jetty", "JETTY-1002: Module ready", false);
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
