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
package be.atbash.runtime;

import be.atbash.runtime.core.data.exception.AtbashStartupAbortException;
import be.atbash.runtime.core.data.watcher.model.ServerMon;
import be.atbash.runtime.logging.LoggingManager;
import be.atbash.runtime.logging.LoggingUtil;

import java.util.Arrays;
import java.util.List;

import static be.atbash.runtime.core.data.parameter.ConfigurationParameters.*;

public class RuntimeMain {

    public static void main(String[] args) {
        // Trace the start of the instance
        long start = System.currentTimeMillis();
        ServerMon serverMon = new ServerMon(start);

        initializeEarlyLogging(args);

        MainRunnerHelper helper = new MainRunnerHelper(args);
        try {
            helper.handleCommandlineArguments();
            if (helper.isDaemonRequested()) {
                helper.startAsDaemon();
                return;
            }

            helper.temporaryWatcherService(serverMon);

            if (LoggingUtil.isVerbose()) {
                helper.logEnvironmentInformation();
            }

            helper.performStartup();

            helper.logStartupTime(start);

            helper.performConfiguration();

            // Now that all Modules are initialized, we can use the real WatcherService and the bean will
            // registered within JMX if the configuration indicates we need to do it.
            helper.registerRuntimeBean(serverMon);

            helper.deployAndRunArchives();

            helper.stopWhenNoApplications();

            helper.handleWarmup();
        } catch (AtbashStartupAbortException abortException) {
            // We need the correct exit status for the process
            System.exit(abortException.getExitStatus());
        }
    }


    private static void initializeEarlyLogging(String[] args) {
        List<String> options = Arrays.asList(args);
        boolean logToConsole = options.contains(LOG_TO_CONSOLE_OPTION);

        boolean verbose = options.contains(VERBOSE_OPTION)
                || options.contains(VERBOSE_OPTION_SHORT);
        LoggingManager.getInstance().initializeEarlyLogging(logToConsole, verbose);
    }

}
