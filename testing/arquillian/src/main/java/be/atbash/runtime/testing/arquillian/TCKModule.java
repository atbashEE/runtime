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
package be.atbash.runtime.testing.arquillian;

import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventPayload;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.core.module.RuntimeObjectsManager;
import be.atbash.runtime.jersey.JerseyModuleConstant;
import be.atbash.runtime.jersey.util.PathUtil;
import be.atbash.runtime.security.jwt.MPJWTModuleConstant;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static be.atbash.runtime.config.mp.MPConfigModuleConstant.ENABLED_FORCED;
import static be.atbash.runtime.config.mp.module.MPConfigModule.MP_CONFIG_MODULE_NAME;

public class TCKModule implements Module<RuntimeConfiguration> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TCKModule.class);
    private RuntimeConfiguration configuration;
    private HandlerCollection handlers;

    @Override
    public String name() {
        return "TCK";
    }

    @Override
    public String[] dependencies() {
        // So that module starts later then Jetty
        return new String[]{"jetty"};  // FIXME make constant
    }

    @Override
    public Specification[] provideSpecifications() {
        return new Specification[]{Specification.TCK};
    }

    @Override
    public Class<? extends Sniffer> moduleSniffer() {
        return TCKSniffer.class;
    }

    @Override
    public List<Class<?>> getRuntimeObjectTypes() {
        return new ArrayList<>();
    }

    @Override
    public <T> T getRuntimeObject(Class<T> exposedObjectType) {
        return null;
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
    public void onEvent(EventPayload eventPayload) {

    }

    @Override
    public void run() {
        handlers = RuntimeObjectsManager.getInstance().getExposedObject(HandlerCollection.class);

        // We force that MPConfig module is always active for the TCK tests.
        configuration.getConfig().getModules().writeConfigValue(MP_CONFIG_MODULE_NAME, ENABLED_FORCED, "true");
    }

    @Override
    public void registerDeployment(ArchiveDeployment deployment) {
        WebAppContext handler = new WebAppContext();
        // MPConfig TCK for example still assumes empty bean.xml -> Discovery ALL
        handler.setInitParameter("org.jboss.weld.environment.servlet.emptyBeansXmlModeAll", "true");

        // If an exception happens during deployment (like CDI issue) make sure we can handle the exception
        //and deployment can be handled as failed from our code.
        handler.setThrowUnavailableOnStartupException(true);

        String contextRoot = deployment.getContextRoot();

        handler.setContextPath(contextRoot);

        handler.setWar(deployment.getDeploymentLocation().getAbsolutePath());
        handler.setParentLoaderPriority(true);  // FIXME Configure

        String packageNames = deployment.getDeploymentData(JerseyModuleConstant.PACKAGE_NAMES);
        if (packageNames != null) {
            // we have JAX-RS endpoints, add jersey servlet.
            String applicationPath = deployment.getDeploymentData(JerseyModuleConstant.APPLICATION_PATH);

            ServletHolder jerseyServlet = handler.addServlet(
                    org.glassfish.jersey.servlet.ServletContainer.class, PathUtil.determinePathForServlet(applicationPath));

            String classNames = deployment.getDeploymentData(JerseyModuleConstant.CLASS_NAMES);
            jerseyServlet.setInitParameter(
                    "jersey.config.server.provider.classnames",
                    classNames);


            // Since we run in embedded and Classpath also contains the TCK jar, multiple JAX-RS resources can be
            // found from within the TCK jar and we don't want them. That is the reason we use explicit class names above.
            List<String> resourcePackages = new ArrayList<>();
            resourcePackages.add("be.atbash.runtime.jersey");

            String realmName = deployment.getDeploymentData(MPJWTModuleConstant.REALM_NAME);
            if (realmName != null) {
                resourcePackages.add("be.atbash.runtime.security.jwt.jaxrs");  // The MP JWT Authentication filter
            }
            String packages = String.join(";", resourcePackages);

            jerseyServlet.setInitParameter(
                    "jersey.config.server.provider.packages",
                    packages);

            jerseyServlet.setInitParameter("jersey.config.server.resource.validation.ignoreErrors", "true");
            jerseyServlet.setInitOrder(0);
        }

        handlers.addHandler(handler);
        try {
            handler.start();
        } catch (Exception e) {
            deployment.setDeploymentException(e);
            return;
        }

        LOGGER.info("JERSEY-104: End of registration of WebApp " + deployment.getDeploymentName());
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
                //throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
                // FIXME Jersey 11 problem (ELContextCleaner ) with EL Spec 5.0

            }
        }
        LOGGER.info("JERSEY-105: Unregistration of WebApp " + deployment.getDeploymentName() + " done");
    }
}
