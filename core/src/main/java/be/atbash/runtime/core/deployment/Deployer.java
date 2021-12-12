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

import be.atbash.runtime.core.data.RunData;
import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.WebAppClassLoader;
import be.atbash.runtime.core.data.deployment.ArchiveContent;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventPayload;
import be.atbash.runtime.core.data.module.event.Events;
import be.atbash.runtime.core.data.module.event.ModuleEventListener;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.core.deployment.monitor.ApplicationMon;
import be.atbash.runtime.core.module.ExposedObjectsModuleManager;
import be.atbash.runtime.monitor.core.MonitorBean;
import be.atbash.runtime.monitor.core.MonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        MonitoringService.registerBean(MonitorBean.ApplicationMonitorBean, applicationMon);
    }

    @Override
    public void onEvent(EventPayload eventPayload) {
        if (Events.DEPLOYMENT.equals(eventPayload.getEventCode())) {
            deployArchive(eventPayload.getPayload());

            // FIXME Marker Rudy
        }
        if (Events.VERIFY_DEPLOYMENT.equals(eventPayload.getEventCode())) {
            verifyArchive(eventPayload.getPayload());
        }

        if (Events.UNDEPLOYMENT.equals(eventPayload.getEventCode())) {
            undeploy(eventPayload.getPayload());
        }
    }

    private void undeploy(String deploymentName) {
        RunData runData = ExposedObjectsModuleManager.getInstance().getExposedObject(RunData.class);
        Optional<ArchiveDeployment> archiveDeployment = runData.getDeployments()
                .stream()
                .filter(ad -> ad.getDeploymentName().equals(deploymentName))
                .findAny();
        if (archiveDeployment.isEmpty()) {
            // FIXME this should never happen as it is already tested
        }
        ArchiveDeployment deployment = archiveDeployment.get();
        deployment.getDeploymentModule().unregisterDeployment(deployment);

        runData.undeployed(deployment);

        applicationMon.unregisterApplication(deployment);

    }

    private void verifyArchive(ArchiveDeployment deployment) {
        File realApplicationDeploymentLocation = new File(runtimeConfiguration.getApplicationDirectory(), deployment.getDeploymentLocation().getAbsolutePath() + "/WEB-INF");
        if (!realApplicationDeploymentLocation.exists()) {
            // Does no longer exists.
            deployment.setDeploymentLocation(null);
        } else {
            // realApplicationDeploymentLocation points to /WEB-INF, we need to go 1 up.
            deployment.setDeploymentLocation(realApplicationDeploymentLocation.getParentFile());
        }

    }

    private void deployArchive(ArchiveDeployment deployment) {
        currentArchiveDeployment = deployment;

        if (deployment.getArchiveFile() == null) {
            if (!loadArchive(deployment)) {
                return;
            }
            feedSniffers(deployment);
        } else {
            if (!unpackArchive(deployment)) {
                // Nothing is deployed,
                return;
            }
            determineSpecifications(deployment);
        }

        determineDeploymentModule(deployment);

        deployment.getDeploymentModule().registerDeployment(deployment);

        deployment.setDeployed();
        //EventManager.getInstance().publishEvent(Events.REGISTER_DEPLOYMENT, deployment);
        RunData runData = ExposedObjectsModuleManager.getInstance().getExposedObject(RunData.class);
        runData.deployed(deployment);

        applicationMon.registerApplication(deployment);
    }

    private void feedSniffers(ArchiveDeployment deployment) {
        List<Sniffer> sniffers = new ArrayList<>(deployment.getSniffers());
        WebAppClassLoader classLoader = deployment.getClassLoader();

        try {
            for (String archiveClass : deployment.getArchiveContent().getArchiveClasses()) {
                Class<?> aClass = classLoader.loadClass(archiveClass);

                List<Sniffer> triggeredSniffers = sniffers.stream()
                        .filter(s -> s.triggered(aClass))
                        .collect(Collectors.toList());

                triggeredSniffers.stream()
                        .filter(Sniffer::isFastDetection)
                        .forEach(sniffers::remove);

                if (sniffers.isEmpty()) {
                    break;
                    // No need to check the rest as all Sniffers are selected
                }
            }
        } catch (ClassNotFoundException e) {
            // FXIME
            e.printStackTrace();
        }
    }

    private boolean loadArchive(ArchiveDeployment deployment) {
        LOG.info(String.format("Loading application %s", deployment.getDeploymentName()));

        Unpack unpack = new Unpack(deployment.getDeploymentLocation());
        ArchiveContent archiveContent = unpack.processExpandedArchive();
        deployment.setArchiveContent(archiveContent);

        extractedsetClassloader(deployment);

        return true;

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
     * Unpacking the WAR into the domain configuration directory.
     *
     * @param deployment
     * @return
     */
    private boolean unpackArchive(ArchiveDeployment deployment) {
        LOG.info(String.format("Deploying application %s", deployment.getArchiveFile()));

        if (!checkDeployment(deployment)) {
            return false;
        }
        File targetLocation = new File(runtimeConfiguration.getApplicationDirectory(), deployment.getDeploymentName() + ".war");
        deployment.setDeploymentLocation(targetLocation);
        Unpack unpack = new Unpack(deployment.getArchiveFile(), targetLocation);
        ArchiveContent archiveContent = unpack.handleArchiveFile();
        deployment.setArchiveContent(archiveContent);

        extractedsetClassloader(deployment);

        return true;
    }

    private void extractedsetClassloader(ArchiveDeployment deployment) {
        WebAppClassLoader appClassLoader = new WebAppClassLoader(deployment.getDeploymentLocation(), Deployer.class.getClassLoader());
        deployment.setClassLoader(appClassLoader);
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
