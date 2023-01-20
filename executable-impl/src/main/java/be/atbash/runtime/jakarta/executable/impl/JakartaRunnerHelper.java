/*
 * Copyright 2021-2023 Rudy De Busscher (https://www.atbash.be)
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
package be.atbash.runtime.jakarta.executable.impl;


import be.atbash.runtime.common.command.AbstractAtbashCommand;
import be.atbash.runtime.core.data.CriticalThreadCount;
import be.atbash.runtime.core.data.RunData;
import be.atbash.runtime.core.data.deployment.ApplicationExecution;
import be.atbash.runtime.core.data.exception.AtbashStartupAbortException;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventManager;
import be.atbash.runtime.core.data.module.event.Events;
import be.atbash.runtime.core.data.parameter.WatcherType;
import be.atbash.runtime.core.data.version.VersionInfo;
import be.atbash.runtime.core.data.watcher.WatcherBean;
import be.atbash.runtime.core.data.watcher.WatcherService;
import be.atbash.runtime.core.data.watcher.model.ServerMon;
import be.atbash.runtime.core.module.RuntimeObjectsManager;
import be.atbash.runtime.jakarta.executable.JakartaRunnerData;
import be.atbash.runtime.jakarta.executable.impl.command.RuntimeCommand;
import be.atbash.runtime.logging.LoggingUtil;
import be.atbash.runtime.logging.earlylog.EarlyLogRecords;
import org.slf4j.Logger;
import picocli.CommandLine;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

/**
 * All steps for the start of the Runtime in methods. The order of the methods is important. see {@link AtbashJakartaRunner}
 */
public class JakartaRunnerHelper {

    private Logger logger;
    private final String[] programArguments;
    private RuntimeCommand actualCommand;
    private RunData runData;

    public JakartaRunnerHelper(String[] programArguments) {

        logger = LoggingUtil.getMainLogger(AtbashJakartaRunner.class);
        this.programArguments = programArguments;
    }

    public void handleCommandlineArguments() {
        if (LoggingUtil.isVerbose()) {
            logger.atTrace().log("CLI-1001");
        }
        RuntimeCommand command = new RuntimeCommand(null);
        CommandLine commandLine = new CommandLine(command);

        actualCommand = (RuntimeCommand) handleCommandLine(programArguments, commandLine);
        if (actualCommand == null) {
            throw new AtbashStartupAbortException(-1);
        }

        validateCommandLine(command);

        if (LoggingUtil.isVerbose()) {
            logger.atTrace().addArgument(actualCommand).log("CLI-1002");
        }

    }

    private void validateCommandLine(RuntimeCommand command) {

        File configDataFile = command.getConfigurationRunnerParameters().getConfigDataFile();
        if (configDataFile != null) {
            if (!configDataFile.exists() || !configDataFile.canRead()) {
                String msg = LoggingUtil.formatMessage(logger, "CLI-114", configDataFile);
                abort(msg, -1);
            }
        }
    }

    private AbstractAtbashCommand handleCommandLine(String[] args, CommandLine commandLine) {
        AbstractAtbashCommand result = null;
        try {
            CommandLine.ParseResult parseResult = commandLine.parseArgs(args);

            List<CommandLine> commandLines = parseResult.asCommandLineList();
            result = commandLines.get(commandLines.size() - 1).getCommand();

        } catch (CommandLine.ParameterException e) {
            Logger logger = LoggingUtil.getMainLogger(AtbashJakartaRunner.class);
            logger.error(e.getMessage());
            String usageMessage = commandLine.getUsageMessage(CommandLine.Help.Ansi.AUTO);
            logger.info(usageMessage);
        }
        return result;
    }


    public void temporaryWatcherService(ServerMon serverMon) {
        // WatcherService only available when ModuleManger starts the modules
        // WatcherService is created by the first Module, the CoreModule.
        // So we create here a temporary just to get some JFR events.
        WatcherService temporaryWatcherService = new WatcherService(WatcherType.MINIMAL);

        VersionInfo versionInfo = VersionInfo.getInstance();

        temporaryWatcherService.logWatcherEvent(Module.CORE_MODULE_NAME, LoggingUtil.formatMessage(logger, "CLI-102", versionInfo.getReleaseVersion()), true);
        serverMon.setVersion(versionInfo.getReleaseVersion());
    }

    public void performStartup() {
        try {
            actualCommand.call();

            // We perform a reset of the entire logging system and thus loggers are reinitialized.
            logger = LoggingUtil.getMainLogger(AtbashJakartaRunner.class);
        } catch (Exception e) {
            // If a problem during Config Module start -> we need to write out the problem
            // If Logging Module is started, we have a log with the issue.
            EarlyLogRecords.getEarlyMessages()
                    .stream()
                    .filter(lr -> lr.getLevel() == Level.SEVERE)
                    .forEach(lr -> logger.error(lr.getMessage().substring(1)));

            logger = LoggingUtil.getMainLogger(AtbashJakartaRunner.class);

            String msg = LoggingUtil.formatMessage(logger, "CLI-107");
            abort(msg, -2);
        }
    }

    public void logStartupTime(long start) {
        long end = System.currentTimeMillis();

        logger.atInfo().addArgument(((double) end - start) / 1000).log("CLI-103");
    }

    public void registerRuntimeBean(ServerMon serverMon) {
        WatcherService watcherService = RuntimeObjectsManager.getInstance().getExposedObject(WatcherService.class);

        runData = RuntimeObjectsManager.getInstance().getExposedObject(RunData.class);
        serverMon.setStartedModules(runData.getStartedModules());
        watcherService.registerBean(WatcherBean.RuntimeWatcherBean, serverMon);

    }

    public void runApplication(JakartaRunnerData runnerData) {

        EventManager eventManager = EventManager.getInstance();
        // FIXME Need some additional parameters
        ApplicationExecution execution = new ApplicationExecution(runnerData.getResources(), runnerData.getRoot());
        execution.setPort(runnerData.getPort());
        execution.setHost(runnerData.getHost());
        runnerData.getApplicationData().forEach(execution::addDeploymentData);

        eventManager.publishEvent(Events.EXECUTION, execution);
    }

    public void handleWarmup() {
        if (actualCommand.getConfigurationRunnerParameters().isWarmup()) {
            CriticalThreadCount.getInstance().waitForCriticalThreadsToFinish();
            String msg = LoggingUtil.formatMessage(logger, "CLI-106");
            abort(msg, 0);
        }
    }

    public void logEnvironmentInformation() {
        logger.atTrace().addArgument(System.getProperty("java.vm.specification.version")).log("CLI-102a");
        logger.atTrace().addArgument(System.getProperty("java.vm.version")).log("CLI-102b");
        logger.atTrace().addArgument(System.getProperty("java.vm.vendor")).log("CLI-102c");
        logger.atTrace().addArgument(System.getProperty("java.vm.name")).log("CLI-102d");
        logger.atTrace().addArgument(System.getProperty("os.name")).log("CLI-102e");

        Runtime runtime = Runtime.getRuntime();
        double mem = runtime.totalMemory() / 1024.0 / 1024.0;

        logger.atTrace().addArgument(mem).log("CLI-102f");
    }

    private void abort(String message, int status) {
        logger.info(message);
        throw new AtbashStartupAbortException(status);
    }
}
