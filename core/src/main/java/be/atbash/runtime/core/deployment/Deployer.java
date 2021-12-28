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
import be.atbash.runtime.core.data.util.ArchiveDeploymentUtil;
import be.atbash.runtime.core.data.watcher.WatcherBean;
import be.atbash.runtime.core.data.watcher.WatcherService;
import be.atbash.runtime.core.deployment.monitor.ApplicationMon;
import be.atbash.runtime.core.module.RuntimeObjectsManager;
import be.atbash.runtime.logging.LoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Deployer implements ModuleEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(Deployer.class);

    private static ArchiveDeployment currentArchiveDeployment;

    private final RuntimeConfiguration runtimeConfiguration;
    private final List<Module> modules;
    private final ApplicationMon applicationMon = new ApplicationMon();

    public Deployer(WatcherService watcherService, RuntimeConfiguration runtimeConfiguration, List<Module> modules) {
        this.runtimeConfiguration = runtimeConfiguration;
        this.modules = modules;
        watcherService.registerBean(WatcherBean.ApplicationWatcherBean, applicationMon);
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
        RunData runData = RuntimeObjectsManager.getInstance().getExposedObject(RunData.class);
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

        deleteDirectory(deployment.getDeploymentLocation());

        applicationMon.unregisterApplication(deployment);

    }

    private boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    private void verifyArchive(ArchiveDeployment deployment) {
        File realApplicationDeploymentLocation = new File(runtimeConfiguration.getApplicationDirectory(), deployment.getDeploymentLocation().getAbsolutePath() + "/WEB-INF");
        if (!realApplicationDeploymentLocation.exists()) {
            // Does no longer exists.
            LOGGER.warn(String.format("DEPLOY-103: Deployment artifact for %s not found", deployment.getDeploymentName()));
            deployment.setDeploymentLocation(null);
        } else {
            // realApplicationDeploymentLocation points to /WEB-INF, we need to go 1 up.
            deployment.setDeploymentLocation(realApplicationDeploymentLocation.getParentFile());
        }

    }

    private void deployArchive(ArchiveDeployment deployment) {
        WatcherService watcherService = RuntimeObjectsManager.getInstance().getExposedObject(WatcherService.class);
        watcherService.logWatcherEvent("Deployer", String.format("DEPLOY-101: Starting deployment of %s", deployment.getDeploymentName()), true);

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

        if (deployment.getDeploymentModule() == null) {
            Logger logger = LoggingUtil.getMainLogger(Deployer.class);
            logger.error(String.format("DEPLOY-107: No module available that can run the deployment '%s'", deployment.getDeploymentName()));
            return;
        }
        deployment.getDeploymentModule().registerDeployment(deployment);

        deployment.setDeployed();
        //EventManager.getInstance().publishEvent(Events.REGISTER_DEPLOYMENT, deployment);
        RunData runData = RuntimeObjectsManager.getInstance().getExposedObject(RunData.class);
        runData.deployed(deployment);

        applicationMon.registerApplication(deployment);
        watcherService.logWatcherEvent("Deployer", String.format("DEPLOY-102: End of deployment of %s", deployment.getDeploymentName()), true);

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
            // FIXME
            e.printStackTrace();
        }
    }

    private boolean loadArchive(ArchiveDeployment deployment) {
        LOGGER.info(String.format("Loading application %s", deployment.getDeploymentName()));

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
                // Don't break, other modules can override this decision.
            }
        }
        // FIXME we also have to consider the following case
        // There is a DeploymentModule found, but what if not all Specifications are satisfied.
        // App with Servlets and JAX-RS started but when we de a restart of the instance, we limit the modules to Jetty.
        // But the app also needs Jersey module.
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
        LOGGER.info(String.format("Deploying application %s", deployment.getArchiveFile()));

        if (!ArchiveDeploymentUtil.testOnArchive(deployment.getArchiveFile())) {
            return false;
        }
        File targetLocation = new File(runtimeConfiguration.getApplicationDirectory(), deployment.getDeploymentName() + ".war");
        deployment.setDeploymentLocation(targetLocation);
        Unpack unpack = new Unpack(deployment.getArchiveFile(), targetLocation);
        ArchiveContent archiveContent = unpack.handleArchiveFile();
        if (archiveContent == null) {
            LOGGER.warn(String.format("DEPLOY-104: Archive is empty or not a valid archive, '%s'", deployment.getDeploymentName()));
            return false;
        }
        deployment.setArchiveContent(archiveContent);

        extractedsetClassloader(deployment);

        return true;
    }

    private void extractedsetClassloader(ArchiveDeployment deployment) {
        WebAppClassLoader appClassLoader = new WebAppClassLoader(deployment.getDeploymentLocation(), Deployer.class.getClassLoader());
        deployment.setClassLoader(appClassLoader);
    }

    /*
    * This is an exception as it is too difficult to expose this through RuntimeObjectsManager.
    * The CoreModule.run() can't create an instance of the Deployer class yet since ModuleManager needs to
    * install all modules first (as Deployer needs all started modules)
    * And having the CoreModule to call a method like this is overhead if we want to enforce RuntimeObjectsManager usage.
     */
    public static ArchiveDeployment getCurrentDeployment() {
        return currentArchiveDeployment;
    }
}
