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
package be.atbash.runtime.core.deployment;

import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.WebAppClassLoader;
import be.atbash.runtime.core.data.deployment.ArchiveContent;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventPayload;
import be.atbash.runtime.core.data.module.event.Events;
import be.atbash.runtime.core.data.module.event.ModuleEventListener;
import be.atbash.runtime.core.deployment.monitor.ApplicationMon;
import be.atbash.runtime.monitor.core.Monitoring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Deployer implements ModuleEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(Deployer.class);

    // FIXME This should be made available to a reworked ExposedObjectsModuleManager
    // Do we need a Core module to be consistent?
    public static ArchiveDeployment currentArchiveDeployment;

    private final RuntimeConfiguration runtimeConfiguration;
    private final List<Module> modules;
    private final ApplicationMon applicationMon = new ApplicationMon();

    public Deployer(RuntimeConfiguration runtimeConfiguration, List<Module> modules) {
        this.runtimeConfiguration = runtimeConfiguration;
        this.modules = modules;
        Monitoring.registerMBean("Atbash.Server.Applications", "data", applicationMon);
    }

    @Override
    public void onEvent(EventPayload eventPayload) {
        if (Events.DEPLOYMENT.equals(eventPayload.getEventCode())) {
            deployArchive(eventPayload.getPayload());

            // FIXME Marker Rudy
        }
    }

    private void deployArchive(ArchiveDeployment deployment) {
        currentArchiveDeployment = deployment;

        if (deployment.isDeployed()) {
            LOG.info(String.format("Loading application %s", deployment.getDeploymentName()));
            // FIXME
        } else {
            if (!deploy(deployment)) {
                // Nothing is deployed,
                return;
            }
            determineSpecifications(deployment);
        }

        determineDeploymentModule(deployment);

        deployment.getDeploymentModule().registerDeployment(deployment);

        //EventManager.getInstance().publishEvent(Events.REGISTER_DEPLOYMENT, deployment);
        applicationMon.registerApplication(deployment.getDeploymentName());
    }

    private void determineDeploymentModule(ArchiveDeployment deployment) {
        Module<?> deployerModule = null;

        List<Specification> specifications = deployment.getSpecifications();
        for (Module<?> module : modules) {
            // Loop over the modules as they are started as this reflects the

            if (matchesSpecification(module, specifications)) {
                deployerModule = module;
                // Don't break, other modules can override this decission.
            }
        }
        deployment.setDeploymentModule(deployerModule);
    }

    private boolean matchesSpecification(Module<?> module, List<Specification> specifications) {
        List<Specification> moduleSpecifications = Arrays.asList(module.provideSpecifications());
        Optional<Specification> matchingSpec = specifications.stream()
                .filter(moduleSpecifications::contains)
                .findAny();
        return matchingSpec.isPresent();
    }

    private void determineSpecifications(ArchiveDeployment deployment) {
        SpecificationChecker specificationChecker = SnifferManager.getInstance().startSpecificationCheck(deployment.getArchiveContent(), deployment.getClassLoader());
        specificationChecker.perform();
        deployment.setSpecifications(specificationChecker.getSpecifications());
        deployment.setSniffers(specificationChecker.getTriggeredSniffers());
    }

    /**
     * Deployment is unpacking the WAR into the domain configuration directory.
     * @param deployment
     * @return
     */
    private boolean deploy(ArchiveDeployment deployment) {
        LOG.info(String.format("Deploying application %s", deployment.getArchiveFile()));

        if (!checkDeployment(deployment)) {
            return false;
        }
        File targetLocation = new File(runtimeConfiguration.getApplicationDirectory(), deployment.getArchiveFile().getName());
        deployment.setDeploymentLocation(targetLocation);
        Unpack unpack = new Unpack(deployment.getArchiveFile(), targetLocation);
        ArchiveContent archiveContent = unpack.handleArchiveFile();
        deployment.setArchiveContent(archiveContent);

        WebAppClassLoader appClassLoader = new WebAppClassLoader(targetLocation, Deployer.class.getClassLoader());
        deployment.setClassLoader(appClassLoader);
        return true;
    }

    private boolean checkDeployment(ArchiveDeployment deployment) {
        // FIXME We need more checks?
        boolean result = deployment.getArchiveFile().exists();
        if (!result) {
            LOG.warn(String.format("DEPLOY-101: Deployment %s not found", deployment.getArchiveFile()));
        }
        return result;
    }
}
