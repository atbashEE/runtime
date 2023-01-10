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

import be.atbash.runtime.core.data.watcher.model.ServerMon;
import be.atbash.runtime.jakarta.executable.JakartaRunner;
import be.atbash.runtime.jakarta.executable.JakartaRunnerData;
import be.atbash.runtime.logging.LoggingManager;
import be.atbash.runtime.logging.LoggingUtil;

import java.util.*;
import java.util.concurrent.CountDownLatch;

import static be.atbash.runtime.core.data.parameter.ConfigurationParameters.*;

public class AtbashJakartaRunner implements JakartaRunner {

    @Override
    public void start(JakartaRunnerData runnerData) {
        long start = System.currentTimeMillis();
        ServerMon serverMon = new ServerMon(start);

        String[] args = defineArguments(runnerData);

        initializeEarlyLogging(args);

        JakartaRunnerHelper helper = new JakartaRunnerHelper(args);
        helper.handleCommandlineArguments();

        helper.temporaryWatcherService(serverMon);

        if (LoggingUtil.isVerbose()) {
            helper.logEnvironmentInformation();
        }

        helper.performStartup();

        helper.logStartupTime(start);

        // Now that all Modules are initialized, we can use the real WatcherService and the bean will
        // registered within JMX if the configuration indicates we need to do it.
        helper.registerRuntimeBean(serverMon);

        helper.runApplication(runnerData);

        helper.handleWarmup();

        preventShutdown();
    }

    private String[] defineArguments(JakartaRunnerData runnerData) {
        List<String> result = new ArrayList<>(runnerData.getCommandLineEntries());

        if (!result.contains(LOG_TO_CONSOLE_OPTION)) {
            result.add(LOG_TO_CONSOLE_OPTION);
        }

        return result.toArray(new String[0]);
    }

    private static void preventShutdown() {
        CountDownLatch preventStop = new CountDownLatch(1);
        new Thread(() -> {
            try {
                preventStop.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private static void initializeEarlyLogging(String[] args) {
        List<String> options = Arrays.asList(args);
        boolean logToConsole = options.contains(LOG_TO_CONSOLE_OPTION);

        boolean verbose = options.contains(VERBOSE_OPTION)
                || options.contains(VERBOSE_OPTION_SHORT);
        LoggingManager.getInstance().initializeEarlyLogging(logToConsole, verbose);
    }
}
