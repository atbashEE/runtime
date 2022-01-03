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
package be.atbash.runtime.embedded;

import be.atbash.runtime.AlphabeticalFileComparator;
import be.atbash.runtime.command.RuntimeCommand;
import be.atbash.runtime.core.data.RunData;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.deployment.info.DeploymentMetadata;
import be.atbash.runtime.core.data.deployment.info.PersistedDeployments;
import be.atbash.runtime.core.data.module.event.EventManager;
import be.atbash.runtime.core.data.module.event.Events;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.core.data.parameter.ConfigurationParameters;
import be.atbash.runtime.core.data.parameter.WatcherType;
import be.atbash.runtime.core.data.util.ArchiveDeploymentUtil;
import be.atbash.runtime.core.data.util.SpecificationUtil;
import be.atbash.runtime.core.data.watcher.WatcherBean;
import be.atbash.runtime.core.data.watcher.WatcherService;
import be.atbash.runtime.core.data.watcher.model.ServerMon;
import be.atbash.runtime.core.deployment.SnifferManager;
import be.atbash.runtime.core.module.ModuleManager;
import be.atbash.runtime.core.module.RuntimeObjectsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class AtbashEmbedded {

    private Logger logger = LoggerFactory.getLogger(AtbashEmbedded.class);

    private RuntimeCommand runtimeCommand;

    private final ConfigurationParameters configurationParameters;

    public AtbashEmbedded(ConfigurationParameters configurationParameters) {
        this.configurationParameters = configurationParameters;
    }

    public AtbashEmbedded() {
        configurationParameters = new ConfigurationParameters();
        configurationParameters.setLogToFile(false);
        configurationParameters.setStateless(true);
        configurationParameters.setWatcher(WatcherType.OFF);
    }

    public void start() {
        long start = System.currentTimeMillis();
        ServerMon serverMon = new ServerMon(start);

        runtimeCommand = new RuntimeCommand(configurationParameters);
        try {
            runtimeCommand.call();
        } catch (Exception e) {
            // FIXME
            e.printStackTrace();
        }

        RunData runData = RuntimeObjectsManager.getInstance().getExposedObject(RunData.class);

        deployAndRunArchives(runData);

        WatcherService watcherService = RuntimeObjectsManager.getInstance().getExposedObject(WatcherService.class);

        serverMon.setStartedModules(runData.getStartedModules());
        watcherService.registerBean(WatcherBean.RuntimeWatcherBean, serverMon);

        logger.info("Atbash Runtime Embedded ready");
    }

    private void deployAndRunArchives(RunData runData) {

        List<ArchiveDeployment> archives = getAllArchivesSpecifiedOnCommandLine(runtimeCommand)
                .stream()
                .map(ArchiveDeployment::new)
                .collect(Collectors.toList());

        EventManager eventManager = EventManager.getInstance();

        List<ArchiveDeployment> persistedDeployments = RuntimeObjectsManager.getInstance()
                .getExposedObject(PersistedDeployments.class)
                .getDeployments()
                .stream()
                .map(md -> createArchiveDeployment(md, eventManager))
                .filter(Objects::nonNull)  // null means the deployment directory with application binaries is gone.
                .collect(Collectors.toList());

        persistedDeployments.forEach(
                a -> eventManager.publishEvent(Events.DEPLOYMENT, a)
        );

        ArchiveDeploymentUtil.assignContextRoots(archives, runtimeCommand.getConfigurationParameters().getContextRoot());

        for (ArchiveDeployment deployment : archives) {
            Optional<ArchiveDeployment> otherDeployment = runData.getDeployments().stream()
                    .filter(ad -> ad.getDeploymentName().equals(deployment.getDeploymentName()))
                    .findAny();
            if (otherDeployment.isPresent()) {
                logger.error(String.format("CLI-109: Deployment %s already active, can't deploy application with same name twice.", deployment.getDeploymentName()));
                System.exit(-2);
            }
            eventManager.publishEvent(Events.DEPLOYMENT, deployment);
        }

    }

    private List<File> getAllArchivesSpecifiedOnCommandLine(RuntimeCommand command) {
        List<File> result = new ArrayList<>();
        File deploymentDirectory = command.getConfigurationParameters().getDeploymentDirectory();
        if (deploymentDirectory != null) {

            result.addAll(findArchivesInDeploymentDirectory(deploymentDirectory));
        }

        File[] archives = command.getConfigurationParameters().getArchives();
        if (archives != null) {
            result.addAll(Arrays.asList(archives));
        }
        return result;
    }

    private List<File> findArchivesInDeploymentDirectory(File deploymentDirectory) {
        List<File> result = new ArrayList<>();
        if (deploymentDirectory.exists() && deploymentDirectory.isDirectory()) {

            File[] files = deploymentDirectory.listFiles((dir, name) -> name.endsWith(".war"));
            if (files != null) {
                result.addAll(Arrays.asList(files));
            }
        } else {
            logger.warn(String.format("CLI-105: %s is not a valid directory", deploymentDirectory));
        }
        result.sort(new AlphabeticalFileComparator());
        return result;
    }

    private static ArchiveDeployment createArchiveDeployment(DeploymentMetadata metadata, EventManager eventManager) {
        List<Sniffer> sniffers = SnifferManager.getInstance().retrieveSniffers(metadata.getSniffers());
        ArchiveDeployment deployment = new ArchiveDeployment(metadata.getDeploymentLocation(), metadata.getDeploymentName()
                , SpecificationUtil.asEnum(metadata.getSpecifications()), sniffers, metadata.getContextRoot(), metadata.getDeploymentData());
        eventManager.publishEvent(Events.VERIFY_DEPLOYMENT, deployment);
        if (deployment.getDeploymentLocation() == null) {
            // The Deployment location is gone
            // FIXME Or should we just do undeploy to handle for example partial corruption?
            RunData runData = RuntimeObjectsManager.getInstance().getExposedObject(RunData.class);
            runData.undeployed(deployment);

            deployment = null;
        }
        return deployment;
    }

    public void stop() {
        ModuleManager manager = ModuleManager.getInstance();
        manager.stopModules();
    }
}
