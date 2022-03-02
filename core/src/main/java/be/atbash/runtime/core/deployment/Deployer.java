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
package be.atbash.runtime.core.deployment;

import be.atbash.runtime.core.data.RunData;
import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.WebAppClassLoader;
import be.atbash.runtime.core.data.deployment.ArchiveContent;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventManager;
import be.atbash.runtime.core.data.module.event.EventPayload;
import be.atbash.runtime.core.data.module.event.Events;
import be.atbash.runtime.core.data.module.event.ModuleEventListener;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.core.data.util.ArchiveDeploymentUtil;
import be.atbash.runtime.core.data.util.FileUtil;
import be.atbash.runtime.core.data.watcher.WatcherBean;
import be.atbash.runtime.core.data.watcher.WatcherService;
import be.atbash.runtime.core.deployment.monitor.ApplicationMon;
import be.atbash.runtime.core.module.RuntimeObjectsManager;
import be.atbash.runtime.logging.LoggingUtil;
import be.atbash.runtime.logging.mapping.BundleMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

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

        // Make it possible to define all messages in the Deployer.properties file.
        BundleMapping.getInstance().addMapping(ArchiveDeploymentUtil.class.getName(), Deployer.class.getName());
    }

    @Override
    public void onEvent(EventPayload eventPayload) {
        if (Events.DEPLOYMENT.equals(eventPayload.getEventCode())) {
            deployArchive(eventPayload.getPayload());
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
            LOGGER.atWarn().addArgument(deployment.getDeploymentName()).log("DEPLOY-103");
            deployment.setDeploymentLocation(null);
        } else {
            // realApplicationDeploymentLocation points to /WEB-INF, we need to go 1 up.
            deployment.setDeploymentLocation(realApplicationDeploymentLocation.getParentFile());
        }

    }

    private void deployArchive(ArchiveDeployment deployment) {
        WatcherService watcherService = RuntimeObjectsManager.getInstance().getExposedObject(WatcherService.class);
        String msg = LoggingUtil.formatMessage(LOGGER, "DEPLOY-101", deployment.getDeploymentName());
        watcherService.logWatcherEvent("Deployer", msg, true);

        currentArchiveDeployment = deployment;

        if (deployment.getArchiveFile() == null) {
            // Deploy an application that was deployed during a previous run.
            // FIXME Use ResourceBundle
            LOGGER.info(String.format("Loading application %s", deployment.getDeploymentName()));
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
            logger.atError().addArgument(deployment.getDeploymentName()).log("DEPLOY-107");

            return;
        }

        EventManager eventManager = EventManager.getInstance();
        eventManager.publishEvent(Events.PRE_DEPLOYMENT, deployment);

        deployment.getDeploymentModule().registerDeployment(deployment);
        if (deployment.getDeploymentException() == null) {
            deployment.setDeployed();

            RunData runData = RuntimeObjectsManager.getInstance().getExposedObject(RunData.class);
            runData.deployed(deployment);

            applicationMon.registerApplication(deployment);
        } else {
            Logger logger = LoggingUtil.getMainLogger(Deployer.class);
            logger.atError()
                    .addArgument(deployment.getDeploymentName())
                    .addArgument(deployment.getDeploymentException().getMessage())
                    .log("DEPLOY-108");

        }
        eventManager.publishEvent(Events.POST_DEPLOYMENT, deployment);
        msg = LoggingUtil.formatMessage(LOGGER, "DEPLOY-102", deployment.getDeploymentName());
        watcherService.logWatcherEvent("Deployer", msg, true);

    }

    private void determineDeploymentModule(ArchiveDeployment deployment) {
        Module<?> deployerModule = null;

        Set<Specification> specifications = deployment.getSpecifications();
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

    private boolean matchesSpecification(Module<?> module, Set<Specification> specifications) {
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
        specificationChecker.getTriggeredSniffers()
                .stream()
                .map(Sniffer::deploymentData)
                .flatMap(map -> map.entrySet().stream())
                .forEach(entry -> deployment.addDeploymentData(entry.getKey(), entry.getValue()));
    }

    /**
     * Unpacking the WAR into the domain configuration directory.
     *
     * @param deployment
     * @return
     */
    private boolean unpackArchive(ArchiveDeployment deployment) {
        LOGGER.info(String.format("Deploying application %s", deployment.getArchiveFile()));

        if (!ArchiveDeploymentUtil.testOnArchive(deployment.getArchiveFile(), true)) {
            return false;
        }

        File targetLocation = defineTargetLocation(deployment);

        deployment.setDeploymentLocation(targetLocation);

        Unpack unpack = new Unpack(deployment.getArchiveFile(), targetLocation);
        ArchiveContent archiveContent = unpack.handleArchiveFile();

        if (archiveContent == null) {
            LOGGER.atWarn().addArgument(deployment.getDeploymentName()).log("DEPLOY-104");
            return false;
        }

        deployment.setArchiveContent(archiveContent);

        setClassloaderForExtractedArchive(deployment);

        return true;
    }

    private File defineTargetLocation(ArchiveDeployment deployment) {
        File targetLocation;
        if (runtimeConfiguration.isStateless()) {
            UUID uuid = UUID.randomUUID();

            targetLocation = new File(FileUtil.getTempDirectory(), deployment.getDeploymentName() + "-" + uuid + ".war");
            if (LoggingUtil.isVerbose()) {
                LOGGER.atTrace()
                        .addArgument(deployment.getDeploymentName())
                        .addArgument(targetLocation)
                        .log("DEPLOY-1001");
            }

        } else {
            targetLocation = new File(runtimeConfiguration.getApplicationDirectory(), deployment.getDeploymentName() + ".war");
        }
        return targetLocation;
    }

    private void setClassloaderForExtractedArchive(ArchiveDeployment deployment) {
        WebAppClassLoader appClassLoader = new WebAppClassLoader(deployment.getDeploymentLocation()
                , deployment.getArchiveContent().getLibraryFiles()
                , Deployer.class.getClassLoader());
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
