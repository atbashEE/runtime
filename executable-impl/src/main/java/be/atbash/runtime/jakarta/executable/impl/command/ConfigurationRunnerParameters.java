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
package be.atbash.runtime.jakarta.executable.impl.command;

import be.atbash.runtime.core.data.parameter.WatcherType;
import picocli.CommandLine;

import java.io.File;

public class ConfigurationRunnerParameters {
    public static final String LOG_TO_CONSOLE_OPTION = "--logToConsole";
    public static final String VERBOSE_OPTION = "--verbose";
    public static final String VERBOSE_OPTION_SHORT = "-v";
    @CommandLine.Option(names = {VERBOSE_OPTION_SHORT, VERBOSE_OPTION}, description = "Start with Verbose logging")
    private boolean verbose = false;

    @CommandLine.Option(names = {"-w", "--watcher"}, description = "Activate the (internal) monitoring tooling")
    private WatcherType watcher = WatcherType.MINIMAL;

    @CommandLine.Option(names = {LOG_TO_CONSOLE_OPTION}, description = "Does the Jakarta Runner logs to the console?")
    private boolean logToConsole = true;

    @CommandLine.Option(names = {"--logConfiguration"}, description = "Points to the logging configuration properties file.")
    private File logConfigurationFile;

    @CommandLine.Option(names = {"--warmup"}, description = "In warmup mode, runtime exists when application(s) are ready.")
    private boolean warmup = false;

    @CommandLine.Option(names = {"--datafile"}, description = "Configuration properties file that are provided to deployer and application")
    private File configDataFile;

    @CommandLine.Option(names = {"-m", "--modules"}, description = "Comma separated list of additional modules that needs to be started.")
    private String additionalModules;

    public Boolean getVerbose() {
        return verbose;
    }

    public void setVerbose(Boolean verbose) {
        this.verbose = verbose;
    }

    public WatcherType getWatcher() {
        return watcher;
    }

    public void setWatcher(WatcherType watcher) {
        this.watcher = watcher;
    }

    public boolean isLogToConsole() {
        return logToConsole;
    }

    public void setLogToConsole(boolean logToConsole) {
        this.logToConsole = logToConsole;
    }

    public File getLogConfigurationFile() {
        return logConfigurationFile;
    }

    public void setLogConfigurationFile(File logConfigurationFile) {
        this.logConfigurationFile = logConfigurationFile;
    }

    public boolean isWarmup() {
        return warmup;
    }

    public void setWarmup(boolean warmup) {
        this.warmup = warmup;
    }

    public File getConfigDataFile() {
        return configDataFile;
    }

    public void setConfigDataFile(File configDataFile) {
        this.configDataFile = configDataFile;
    }

    public String getAdditionalModules() {
        return additionalModules;
    }

    public void setAdditionalModules(String additionalModules) {
        this.additionalModules = additionalModules;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ConfigurationParameters{");
        sb.append(", --verbose=").append(verbose);
        sb.append(", --watcher=").append(watcher);
        sb.append(", --logToConsole=").append(logToConsole);
        sb.append(", --logConfigurationFile=").append(logConfigurationFile);
        sb.append(", --warmup=").append(warmup);
        if (configDataFile != null) {
            sb.append(", --datafile=").append(configDataFile);
        }
        sb.append('}');
        return sb.toString();
    }
}
