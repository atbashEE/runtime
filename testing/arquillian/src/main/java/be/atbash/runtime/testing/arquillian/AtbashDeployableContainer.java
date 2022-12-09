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

import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.module.event.EventManager;
import be.atbash.runtime.core.data.module.event.Events;
import be.atbash.runtime.core.data.parameter.ConfigurationParameters;
import be.atbash.runtime.core.data.parameter.WatcherType;
import be.atbash.runtime.core.data.util.FileUtil;
import be.atbash.runtime.core.data.util.StringUtil;
import be.atbash.runtime.core.module.RuntimeObjectsManager;
import be.atbash.runtime.embedded.AtbashEmbedded;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;

/**
 * Implementation of Arquillian {@code DeployableContainer} for use with Atbash Embedded.
 */
public class AtbashDeployableContainer implements DeployableContainer<AtbashContainerConfiguration> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AtbashDeployableContainer.class);

    private AtbashContainerConfiguration atbashContainerConfiguration;
    private AtbashEmbedded embedded;

    @Inject
    @ApplicationScoped
    private InstanceProducer<BeanManager> beanManagerInstance;

    @Override
    public Class<AtbashContainerConfiguration> getConfigurationClass() {
        return AtbashContainerConfiguration.class;
    }

    @Override
    public void setup(AtbashContainerConfiguration atbashContainerConfiguration) {
        this.atbashContainerConfiguration = atbashContainerConfiguration;
    }

    @Override
    public void start() throws LifecycleException {
        startAtbashEmbedded();
    }

    private void startAtbashEmbedded() {
        ConfigurationParameters configurationParameters = new ConfigurationParameters();
        configurationParameters.setLogToFile(false);
        configurationParameters.setStateless(true);
        configurationParameters.setWatcher(WatcherType.OFF);
        configurationParameters.setProfile(atbashContainerConfiguration.getProfile());
        configurationParameters.setModules(atbashContainerConfiguration.getModules());

        embedded = new AtbashEmbedded(configurationParameters);
        embedded.withTCKModule();  // TCK module defines a specific deployer.
        embedded.start();
    }

    @Override
    public void stop() throws LifecycleException {
        embedded.stop();
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Servlet 5.0");
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        return deployArchive(archive);
    }

    private ProtocolMetaData deployArchive(Archive<?> archive) throws DeploymentException {
        // Write archive to disk
        File archiveLocation = defineArchiveLocation(archive.getName());
        archive.as(ZipExporter.class).exportTo(
                archiveLocation, true);

        // Start deployment by emitting event.
        EventManager eventManager = EventManager.getInstance();
        ArchiveDeployment deployment = new ArchiveDeployment(archiveLocation);
        eventManager.publishEvent(Events.DEPLOYMENT, deployment);

        // Somehow, we need to set the CDI BeanManager into the Arquillian Manager so that CDI testEnricher finds it
        // FIXME This will fail if we switch to a real CDI lite implementation that doesn't has BeanManager implemented?
        beanManagerInstance.set(CDI.current().getBeanManager());
        // TODO Should we use org.jboss.arquillian.testenricher.cdi.container.BeanManagerProducer?

        // Check if the deployment was successful.
        Exception deploymentException = deployment.getDeploymentException();
        if (deploymentException != null) {
            if (deploymentException instanceof DeploymentException) {
                throw (DeploymentException) deploymentException;
            } else {
                throw new DeploymentException("Deployment Failed", deploymentException);
            }
        }

        // Define ProtocolMetaData
        ProtocolMetaData result = new ProtocolMetaData();

        HTTPContext http = new HTTPContext("localhost", 8080);  // TODO does this needs to be configurable?
        HandlerCollection handlers = RuntimeObjectsManager.getInstance().getExposedObject(HandlerCollection.class);

        // Find WebAppContext for the app that we deployed.
        Optional<Handler> handler = Arrays.stream(handlers.getHandlers())
                .filter(h -> h instanceof WebAppContext)
                .filter(wac -> ((WebAppContext) wac).getContextPath().equals(deployment.getContextRoot()))
                .findAny();
        if (handler.isEmpty()) {
            throw new DeploymentException("WebAppContext not found for " + deployment.getContextRoot());
        }

        // Retrieve all URL endpoints of Servlets for Arqullian.
        ServletHandler servletHandler = ((WebAppContext) handler.get()).getServletHandler();
        for (ServletHolder servlet : servletHandler.getServlets()) {
            if (servlet.isAvailable()) {
                http.add(new Servlet(servlet.getName(), servlet.getContextPath()));
            } else {
                LOGGER.info(String.format("The following servlet in not available for deployment %s : %s", servlet.getName(), deployment.getDeploymentName()));
            }
        }

        result.addContext(http);
        return result;
    }

    private File defineArchiveLocation(String archiveName) {
        return new File(FileUtil.getTempDirectory() + "/" + archiveName);
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        undeployArchive(archive);
    }

    private void undeployArchive(Archive<?> archive) {
        EventManager eventManager = EventManager.getInstance();
        eventManager.publishEvent(Events.UNDEPLOYMENT, StringUtil.determineDeploymentName(archive.getName()));

        if (!atbashContainerConfiguration.isKeepArchive()) {
            File archiveLocation = defineArchiveLocation(archive.getName());
            boolean deleted = archiveLocation.delete();
            if (!deleted) {
                LOGGER.warn(String.format("Unable to delete the archive from the location '%s'", archiveLocation));
            }
        }
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Implement Deploy of Descriptor");
    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Implement Undeploy of Descriptor");
    }
}
