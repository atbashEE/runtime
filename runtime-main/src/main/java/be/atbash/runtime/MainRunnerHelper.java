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
package be.atbash.runtime;

import be.atbash.runtime.command.RuntimeCommand;
import be.atbash.runtime.common.command.AbstractAtbashCommand;
import be.atbash.runtime.core.data.CriticalThreadCount;
import be.atbash.runtime.core.data.RunData;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.deployment.info.DeploymentMetadata;
import be.atbash.runtime.core.data.deployment.info.PersistedDeployments;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventManager;
import be.atbash.runtime.core.data.module.event.Events;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.core.data.parameter.WatcherType;
import be.atbash.runtime.core.data.util.ArchiveDeploymentUtil;
import be.atbash.runtime.core.data.util.SpecificationUtil;
import be.atbash.runtime.core.data.version.VersionInfo;
import be.atbash.runtime.core.data.watcher.WatcherBean;
import be.atbash.runtime.core.data.watcher.WatcherService;
import be.atbash.runtime.core.data.watcher.model.ServerMon;
import be.atbash.runtime.core.deployment.SnifferManager;
import be.atbash.runtime.core.module.RuntimeObjectsManager;
import be.atbash.runtime.logging.LoggingUtil;
import be.atbash.runtime.logging.earlylog.EarlyLogRecords;
import org.slf4j.Logger;
import picocli.CommandLine;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * All steps for the start of the Runtime in methods. The order of the methods is important. see {@link RuntimeMain}
 */
public class MainRunnerHelper {

    private Logger logger;
    private final String[] programArguments;
    private RuntimeCommand actualCommand;
    private RunData runData;

    public MainRunnerHelper(String[] programArguments) {

        logger = LoggingUtil.getMainLogger(RuntimeMain.class);
        this.programArguments = programArguments;
    }

    public void handleCommandlineArguments() {
        if (LoggingUtil.isVerbose()) {
            logger.trace("CLI-1001: Handling command line arguments");
        }
        RuntimeCommand command = new RuntimeCommand(null);
        CommandLine commandLine = new CommandLine(command);

        actualCommand = (RuntimeCommand) handleCommandLine(programArguments, commandLine);
        if (actualCommand == null) {
            System.exit(-1);
        }

        if (!validateCommandLine(command)) {
            logger.error("CLI-111: Number of values for parameter --contextroot does not math number of application to be deployed.");
            System.exit(-1);
        }

        if (LoggingUtil.isVerbose()) {
            logger.trace(String.format("CLI-1002: Command line arguments in use %s", actualCommand));
        }

    }

    private boolean validateCommandLine(RuntimeCommand command) {
        String contextRoot = command.getConfigurationParameters().getContextRoot();
        if (contextRoot.isBlank()) {
            // No contextroot value specified, nothing to check.
            return true;
        }

        List<File> archivesSpecifiedOnCommandLine = getAllArchivesSpecifiedOnCommandLine(command);
        String[] parts = contextRoot.split(",");
        return archivesSpecifiedOnCommandLine.size() == parts.length;
    }

    private AbstractAtbashCommand handleCommandLine(String[] args, CommandLine commandLine) {
        AbstractAtbashCommand result = null;
        try {
            CommandLine.ParseResult parseResult = commandLine.parseArgs(args);

            List<CommandLine> commandLines = parseResult.asCommandLineList();
            result = commandLines.get(commandLines.size() - 1).getCommand();

        } catch (CommandLine.ParameterException e) {
            Logger logger = LoggingUtil.getMainLogger(RuntimeMain.class);
            logger.error(e.getMessage());
            String usageMessage = commandLine.getUsageMessage(CommandLine.Help.Ansi.AUTO);
            logger.info(usageMessage);
        }
        return result;
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

    public void temporaryWatcherService(ServerMon serverMon) {
        // WatcherService only available when ModuleManger starts the modules
        // WatcherService is created by the first Module, the CoreModule.
        // So we create here a temporary just to get some JFR events.
        WatcherService temporaryWatcherService = new WatcherService(WatcherType.MINIMAL);

        VersionInfo versionInfo = VersionInfo.getInstance();
        temporaryWatcherService.logWatcherEvent(Module.CORE_MODULE_NAME, String.format("CLI-102: Starting Atbash Runtime version %s", versionInfo.getReleaseVersion()), true);
        serverMon.setVersion(versionInfo.getReleaseVersion());
    }

    public void performStartup() {
        try {
            actualCommand.call();

            // We perform a reset of the entire logging system and thus loggers are reinitialized.
            logger = LoggingUtil.getMainLogger(RuntimeMain.class);
        } catch (Exception e) {
            // If a problem during Config Module start -> we need to write out the problem
            // If Logging Module is started, we have a log with the issue.
            EarlyLogRecords.getEarlyMessages()
                    .stream()
                    .filter(lr -> lr.getLevel() == Level.SEVERE)
                    .forEach(lr -> logger.error(lr.getMessage().substring(1)));

            logger = LoggingUtil.getMainLogger(RuntimeMain.class);

            logger.info("CLI-107: Atbash Runtime startup aborted due to previous errors. (See log if created for the reason of the abort)");
            System.exit(-2);
        }
    }

    public void logStartupTime(long start) {
        long end = System.currentTimeMillis();

        logger.info("CLI-103: Started Atbash Runtime in " + ((double) end - start) / 1000 + " secs");
    }

    public void registerRuntimeBean(ServerMon serverMon) {
        WatcherService watcherService = RuntimeObjectsManager.getInstance().getExposedObject(WatcherService.class);

        runData = RuntimeObjectsManager.getInstance().getExposedObject(RunData.class);
        serverMon.setStartedModules(runData.getStartedModules());
        watcherService.registerBean(WatcherBean.RuntimeWatcherBean, serverMon);

    }

    public void deployAndRunArchives() {

        List<ArchiveDeployment> archives = getAllArchivesSpecifiedOnCommandLine(actualCommand)
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

        ArchiveDeploymentUtil.assignContextRoots(archives, actualCommand.getConfigurationParameters().getContextRoot());

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

    private static ArchiveDeployment createArchiveDeployment(DeploymentMetadata metadata, EventManager eventManager) {
        List<Sniffer> sniffers = SnifferManager.getInstance().retrieveSniffers(metadata.getSniffers());
        ArchiveDeployment deployment = new ArchiveDeployment(metadata.getDeploymentLocation(), metadata.getDeploymentName()
                , SpecificationUtil.asEnum(metadata.getSpecifications()), sniffers, metadata.getContextRoot());
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

    public void stopWhenNoApplications() {
        int applications = runData.getDeployments().size();

        if (applications > 0) {
            logger.info(String.format("CLI-104: %s Applications running", applications));
        } else {

            logger.warn("CLI-105: No Applications running");
            if (!runData.isDomainMode()) {
                logger.info("CLI-108: Atbash Runtime stopped as there are no applications deployed and Runtime is not in domain mode.");
                System.exit(-2);
            }
        }
    }

    public void handleWarmup() {
        if (actualCommand.getConfigurationParameters().isWarmup()) {
            CriticalThreadCount.getInstance().waitForCriticalThreadsToFinish();
            logger.info("CLI-106: process stop due to warmup parameter");
            System.exit(0);  // Normal status.
        }
    }

    public void logEnvironmentInformation() {
        logger.trace(String.format("CLI-102: JDK Version 'JDK %s'", System.getProperty("java.vm.specification.version")));
        logger.trace(String.format("CLI-102: Full version '%s'", System.getProperty("java.vm.version")));
        logger.trace(String.format("CLI-102: JVM Vendor '%s'", System.getProperty("java.vm.vendor")));
        logger.trace(String.format("CLI-102: JVM name '%s'", System.getProperty("java.vm.name")));

        logger.trace(String.format("CLI-102: OS name '%s'", System.getProperty("os.name")));

        Runtime runtime = Runtime.getRuntime();
        double mem = runtime.totalMemory() / 1024.0 / 1024.0;


        logger.trace(String.format("CLI-102: Java memory %.0fMB", mem));
    }

}
