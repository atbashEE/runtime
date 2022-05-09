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

import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.exception.UnexpectedException;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventPayload;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.core.data.watcher.WatcherService;
import be.atbash.runtime.core.module.RuntimeObjectsManager;
import be.atbash.runtime.jersey.util.PathUtil;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
    public List<Class<?>> getRuntimeObjectTypes() {
        return Collections.emptyList();
    }

    @Override
    public <T> T getRuntimeObject(Class<T> exposedObjectType) {
        return null;
    }

    @Override
    public Specification[] provideSpecifications() {
        return new Specification[]{Specification.REST};
    }

    @Override
    public Class<? extends Sniffer> moduleSniffer() {
        return RestSniffer.class;
    }

    @Override
    public void onEvent(EventPayload eventPayload) {

    }

    @Override
    public void registerDeployment(ArchiveDeployment deployment) {
        WebAppContext handler = new WebAppContext();
        // If an exception happens during deployment (like CDI issue) make sure we can handle the exception
        //and deployment can be handled as failed from our code.
        handler.setThrowUnavailableOnStartupException(true);

        String contextRoot = deployment.getContextRoot();

        handler.setContextPath(contextRoot);

        handler.setWar(deployment.getDeploymentLocation().getAbsolutePath());
        handler.setParentLoaderPriority(true);  // FIXME Configure


        // TODO: testing required -> So that we have a CDI container for each deployment?
        //handler.setInitParameter("WELD_CONTEXT_ID_KEY", deployment.getDeploymentName());

        String applicationPath = deployment.getDeploymentData(JerseyModuleConstant.APPLICATION_PATH);

        ServletHolder jerseyServlet = handler.addServlet(
                org.glassfish.jersey.servlet.ServletContainer.class, PathUtil.determinePathForServlet(applicationPath));

        String packages = determinePackageNames(deployment);

        jerseyServlet.setInitParameter(
                "jersey.config.server.provider.packages",
                packages);

        jerseyServlet.setInitOrder(0);


        handlers.addHandler(handler);
        try {
            handler.start();
        } catch (Exception e) {
            deployment.setDeploymentException(e);
            return;
        }

        LOGGER.atInfo().addArgument(deployment.getDeploymentName()).log("JERSEY-104");
    }

    private String determinePackageNames(ArchiveDeployment deployment) {
        List<String> resourcePackages = new ArrayList<>(Arrays.asList(deployment.getDeploymentData(JerseyModuleConstant.PACKAGE_NAMES).split(",")));
        resourcePackages.add("be.atbash.runtime.jersey");

        String extraPackageNames = deployment.getDeploymentData(JerseyModuleConstant.EXTRA_PACKAGE_NAMES);
        if (extraPackageNames != null) {

            resourcePackages.addAll(Arrays.asList(extraPackageNames.split(",")));
        }
        return String.join(";", resourcePackages);
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
        LOGGER.info("JERSEY-105: Unregistration of WebApp " + deployment.getDeploymentName() + " done");
    }

    @Override
    public void run() {
        WatcherService watcherService = RuntimeObjectsManager.getInstance().getExposedObject(WatcherService.class);
        watcherService.logWatcherEvent("Jersey", "JERSEY-1001: Module startup", false);

        handlers = RuntimeObjectsManager.getInstance().getExposedObject(HandlerCollection.class);

        watcherService.logWatcherEvent("Jersey", "JERSEY-1002: Module ready", false);

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
