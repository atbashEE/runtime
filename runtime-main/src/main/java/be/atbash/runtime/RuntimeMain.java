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
package be.atbash.runtime;

import be.atbash.runtime.common.command.AbstractAtbashCommand;
import be.atbash.runtime.common.command.RuntimeCommand;
import be.atbash.runtime.core.data.CriticalThreadCount;
import be.atbash.runtime.core.data.RunData;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.deployment.info.DeploymentMetadata;
import be.atbash.runtime.core.data.deployment.info.PersistedDeployments;
import be.atbash.runtime.core.data.module.event.EventManager;
import be.atbash.runtime.core.data.module.event.Events;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.core.data.util.SpecificationUtil;
import be.atbash.runtime.core.data.version.VersionInfo;
import be.atbash.runtime.core.deployment.SnifferManager;
import be.atbash.runtime.core.module.ExposedObjectsModuleManager;
import be.atbash.runtime.logging.LoggingManager;
import be.atbash.runtime.logging.earlylog.EarlyLogRecords;
import be.atbash.runtime.monitor.core.MonitorBean;
import be.atbash.runtime.monitor.data.ServerMon;
import be.atbash.runtime.monitor.core.MonitoringService;
import org.slf4j.Logger;
import picocli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class RuntimeMain {

    private static Logger LOGGER;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        ServerMon serverMon = new ServerMon(start);

        boolean logToConsole = Arrays.asList(args).contains("--logToConsole");
        LoggingManager.getInstance().initializeEarlyLogging(logToConsole);

        // We can't create a logger before we have installed our EarlyLogHandler
        LOGGER = LoggingManager.getInstance().getMainLogger(RuntimeMain.class, logToConsole);

        LOGGER.info("CLI-101: Handling command line arguments");
        RuntimeCommand command = new RuntimeCommand();
        CommandLine commandLine = new CommandLine(command);

        AbstractAtbashCommand actualCommand = handleCommandLine(args, command, commandLine);
        if (actualCommand == null) {
            return;
            // FIXME
        }

        if (!validateCommandLine(command)) {
            LOGGER.error("CLI-111: Number of values for parameter --contextroot does not math number of application to be deployed.");
        }

        if (actualCommand.getCommandType() == AbstractAtbashCommand.CommandType.CLI) {
            try {
                LoggingManager.getInstance().restoreOriginalHandlers();
                actualCommand.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {

            MonitoringService.setActive(command.getConfigurationParameters().isWatcher());

            VersionInfo versionInfo = VersionInfo.getInstance();
            MonitoringService.logMonitorEvent("Main", String.format("CLI-102: Starting Atbash Runtime version %s", versionInfo.getReleaseVersion()));
            serverMon.setVersion(versionInfo.getReleaseVersion());

            MonitoringService.registerBean(MonitorBean.RuntimeMonitorBean, serverMon);

            try {
                actualCommand.call();
            } catch (Exception e) {
                // If a problem during Config Module start -> we need to write out the problem
                // If Logging Module is started, we have a log with the issue.
                EarlyLogRecords.getEarlyMessages()
                        .stream()
                        .filter(lr -> lr.getLevel() == Level.SEVERE)
                        .forEach(lr -> LOGGER.error(lr.getMessage().substring(1)));
                // Why do we loose the handler on our Logger?
                LOGGER = LoggingManager.getInstance().getMainLogger(RuntimeMain.class, logToConsole);

                LOGGER.info("CLI-107: Atbash Runtime startup aborted due to previous errors. (See log if created for the reason of the abort)");
                return;
            }

            long end = System.currentTimeMillis();
            // Why do we loose the handler on our Logger?
            LOGGER = LoggingManager.getInstance().getMainLogger(RuntimeMain.class, logToConsole);
            LOGGER.info("CLI-103: Started Atbash Runtime in " + ((double) end - start) / 1000 + " secs");

             deployAndRunArchives(command);

             RunData runData = ExposedObjectsModuleManager.getInstance().getExposedObject(RunData.class);
            int applications = runData.getDeployments().size();

            if (applications > 0) {
                LOGGER.info(String.format("CLI-104: %s Applications running", applications));
            } else {

                LOGGER.warn("CLI-105: No Applications running");
                if (!runData.isDomainMode()) {
                    LOGGER.info("CLI-108: Atbash Runtime stopped as there are no applications deployed and Runtime is not in domain mode.");
                    System.exit(0);  // Normal status.
                }
            }
        }

        if (command.getConfigurationParameters().isWarmup()) {
            CriticalThreadCount.getInstance().waitForCriticalThreadsToFinish();
            LOGGER.info("CLI-106: process stop due to warmup parameter");
            System.exit(0);  // Normal status.
        }
    }

    private static boolean validateCommandLine(RuntimeCommand command) {
        String contextRoot = command.getConfigurationParameters().getContextRoot();
        if (contextRoot.isBlank()) {
            // No contextroot value specified, nothing to check.
            return true;
        }

        List<File> archivesSpecifiedOnCommandLine = getAllArchivesSpecifiedOnCommandLine(command);
        String[] parts = contextRoot.split(",");
        return archivesSpecifiedOnCommandLine.size() == parts.length;
    }

    private static AbstractAtbashCommand handleCommandLine(String[] args, RuntimeCommand command, CommandLine commandLine) {
        AbstractAtbashCommand result = null;
        try {
            CommandLine.ParseResult parseResult = commandLine.parseArgs(args);

            List<CommandLine> commandLines = parseResult.asCommandLineList();
            result = commandLines.get(commandLines.size() - 1).getCommand();

        } catch (CommandLine.ParameterException e) {
            System.out.println(e.getMessage());
            commandLine.printVersionHelp(System.out, CommandLine.Help.Ansi.AUTO);
        }
        return result;
    }

    private static void deployAndRunArchives(RuntimeCommand command) {

        List<ArchiveDeployment> archives = getAllArchivesSpecifiedOnCommandLine(command)
                .stream()
                .map(ArchiveDeployment::new)
                .collect(Collectors.toList());

        EventManager eventManager = EventManager.getInstance();

        List<ArchiveDeployment> persistedDeployments = ExposedObjectsModuleManager.getInstance()
                .getExposedObject(PersistedDeployments.class)
                .getDeployments()
                .stream()
                .map(md -> createArchiveDeployment(md, eventManager))
                .filter(Objects::nonNull)  // null means the deployment directory with application binaries is gone.
                .collect(Collectors.toList());

        persistedDeployments.forEach(
                a -> eventManager.publishEvent(Events.DEPLOYMENT, a)
        );

        ArchiveDeploymentUtil.assignContextRoots(archives, command.getConfigurationParameters().getContextRoot());

        if (!archives.isEmpty()) {
            archives.forEach(a -> eventManager.publishEvent(Events.DEPLOYMENT, a));
        }
    }


    private static ArchiveDeployment createArchiveDeployment(DeploymentMetadata metadata, EventManager eventManager) {
        List<Sniffer> sniffers = SnifferManager.getInstance().retrieveSniffers(metadata.getSniffers());
        ArchiveDeployment deployment = new ArchiveDeployment(metadata.getDeploymentLocation(), metadata.getDeploymentName()
                , SpecificationUtil.asEnum(metadata.getSpecifications()), sniffers, metadata.getContextRoot());
        eventManager.publishEvent(Events.VERIFY_DEPLOYMENT, deployment);
        if (deployment.getDeploymentLocation() == null) {
            // The Deployment location is gone
            // FIXME remove from persistedDeployments.
            deployment = null;
        }
        return deployment;
    }

    private static List<File> getAllArchivesSpecifiedOnCommandLine(RuntimeCommand command) {
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

    private static List<File> findArchivesInDeploymentDirectory(File deploymentDirectory) {
        List<File> result = new ArrayList<>();
        if (deploymentDirectory.exists() && deploymentDirectory.isDirectory()) {

            File[] files = deploymentDirectory.listFiles((dir, name) -> name.endsWith(".war"));
            if (files != null) {
                result.addAll(Arrays.asList(files));
            }
        } else {
            LOGGER.warn(String.format("CLI-105: %s is not a valid directory", deploymentDirectory));
        }
        result.sort(new AlphabeticalComparator());
        return result;
    }

    private static class AlphabeticalComparator implements java.util.Comparator<File> {

        @Override
        public int compare(File f1, File f2) {
            return f1.getName().compareTo(f2.getName());
        }
    }
}
