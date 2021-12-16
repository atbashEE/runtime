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
package be.atbash.runtime.core.data.parameter;

import picocli.CommandLine;

import java.io.File;

public class ConfigurationParameters {
    @CommandLine.Option(names = {"-d", "--daemon"}, description = "Start as background task")
    private boolean daemon = false;

    @CommandLine.Option(names = {"-p", "--profile"}, description = "Profile name")
    private String profile = "default";

    @CommandLine.Option(names = {"-m", "--modules"}, description = "Comma separated list of modules that needs to be started.")
    private String modules;

    @CommandLine.Option(names = {"-v", "--verbose"}, description = "Start with Verbose logging")
    private boolean verbose = false;

    @CommandLine.Option(names = {"-w", "--watcher"}, description = "Activate the (internal) monitoring tooling")
    private WatcherType watcher = WatcherType.MINIMAL;

    @CommandLine.Option(names = {"-r", "--root"}, description = "Location of the Atbash runtime installation, current directory by default")
    private String rootDirectory = ".";

    @CommandLine.Option(names = {"--deploymentdirectory"}, description = "Deployment directory that is searched for WAR files which needs to be deployed.")
    private File deploymentDirectory;

    @CommandLine.Option(names = {"-n", "--configName"}, description = "Configuration name")
    private String configName = "default";

    @CommandLine.Option(names = {"--logToConsole"}, description = "Does the runtime logs to the console")
    private boolean logToConsole = false;

    @CommandLine.Option(names = {"--warmup"}, description = "In warmup mode, runtime exists when application(s) are ready.")
    private boolean warmup = false;

    @CommandLine.Option(names = {"--contextroot"}, description = "The context root for the application. Comma separated list when multiple applications are deployed.")
    private String contextRoot = "";

    @CommandLine.Parameters(index = "0..*")
    private File[] archives;

    public boolean isDaemon() {
        return daemon;
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getModules() {
        return modules;
    }

    public void setModules(String modules) {
        this.modules = modules;
    }

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

    public String getRootDirectory() {
        return rootDirectory;
    }

    public void setRootDirectory(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public File getDeploymentDirectory() {
        return deploymentDirectory;
    }

    public void setDeploymentDirectory(File deploymentDirectory) {
        this.deploymentDirectory = deploymentDirectory;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public boolean isLogToConsole() {
        return logToConsole;
    }

    public void setLogToConsole(boolean logToConsole) {
        this.logToConsole = logToConsole;
    }

    public boolean isWarmup() {
        return warmup;
    }

    public void setWarmup(boolean warmup) {
        this.warmup = warmup;
    }

    public String getContextRoot() {
        return contextRoot;
    }

    public void setContextRoot(String contextRoot) {
        this.contextRoot = contextRoot;
    }

    public File[] getArchives() {
        return archives;
    }

    public void setArchives(File[] archives) {
        this.archives = archives;
    }
}
