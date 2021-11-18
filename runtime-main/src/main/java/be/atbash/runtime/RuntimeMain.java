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
import be.atbash.runtime.core.data.RunData;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.module.event.EventManager;
import be.atbash.runtime.core.data.module.event.Events;
import be.atbash.runtime.core.data.version.VersionInfo;
import be.atbash.runtime.core.module.ExposedObjectsModuleManager;
import be.atbash.runtime.logging.LoggingManager;
import be.atbash.runtime.monitor.ServerMon;
import be.atbash.runtime.monitor.core.Monitoring;
import org.slf4j.Logger;
import picocli.CommandLine;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class RuntimeMain {

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        ServerMon serverMon = new ServerMon(start);

        boolean logToConsole = Arrays.asList(args).contains("--logToConsole");
        LoggingManager.getInstance().initializeEarlyLogging(logToConsole);

        // We can't create a logger before we have installed our EarlyLogHandler
        Logger LOGGER = LoggingManager.getInstance().getMainLogger(RuntimeMain.class, logToConsole);

        LOGGER.info("CLI-101: Handling command line arguments");
        RuntimeCommand command = new RuntimeCommand();
        CommandLine commandLine = new CommandLine(command);

        AbstractAtbashCommand actualCommand = handleCommandLine(args, command, commandLine);
        if (actualCommand == null) {
            return;
            // FIXME
        }

        if (actualCommand.getCommandType() == AbstractAtbashCommand.CommandType.CLI) {
            try {
                LoggingManager.getInstance().restoreOriginalHandlers();
                actualCommand.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {

            Monitoring.setActive(command.getConfigurationParameters().isWatcher());

            VersionInfo versionInfo = VersionInfo.getInstance();
            Monitoring.logMonitorEvent("Main", String.format("CLI-102: Starting Atbash Runtime version %s", versionInfo.getReleaseVersion()));
            serverMon.setVersion(versionInfo.getReleaseVersion());

            Monitoring.registerMBean("Atbash.Server", "Info", serverMon);

            try {
                actualCommand.call();
            } catch (Exception e) {
                e.printStackTrace();
            }

            long end = System.currentTimeMillis();
            // Why do we loose the handler on our Logger?
            LOGGER = LoggingManager.getInstance().getMainLogger(RuntimeMain.class, logToConsole);
            LOGGER.info("CLI-103: Started Atbash Runtime in " + ((double) end - start) / 1000 + " secs");

            int applications = deployAndRunArchives(command);
            // FIXME CLI-104 is used twice
            if (applications > 0) {
                LOGGER.info(String.format("CLI-104: %s Applications running", applications));
            } else {
                // FIXME, if we do not run the domain mode (domain module is running) this
                // doesn't make any sense and we should quit the process
                LOGGER.warn("CLI-105: No Applications running");
            }
        }
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

    private static int deployAndRunArchives(RuntimeCommand command) {
        File[] archives = command.getConfigurationParameters().getArchives();
        EventManager eventManager = EventManager.getInstance();
        if (archives != null && archives.length > 0) {
            Arrays.stream(archives).forEach(a -> eventManager.publishEvent(Events.DEPLOYMENT, new ArchiveDeployment(a)));
        }
        RunData runData = ExposedObjectsModuleManager.getInstance().getExposedObject(RunData.class);
        return runData.getDeployments().size();
    }
}
