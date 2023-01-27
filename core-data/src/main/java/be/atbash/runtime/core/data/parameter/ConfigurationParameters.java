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
package be.atbash.runtime.core.data.parameter;

import picocli.CommandLine;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ConfigurationParameters {
    public static final String LOG_TO_CONSOLE_OPTION = "--logToConsole";
    public static final String VERBOSE_OPTION = "--verbose";
    public static final String VERBOSE_OPTION_SHORT = "-v";

    @CommandLine.Option(names = {"-d", "--daemon"}, description = "Start as background task")
    private boolean daemon = false;

    @CommandLine.Option(names = {"-p", "--profile"}, description = "Profile name")
    private String profile = "default";

    @CommandLine.Option(names = {"-m", "--modules"}, description = "Comma separated list of modules that needs to be started.")
    private String modules;

    @CommandLine.Option(names = {VERBOSE_OPTION_SHORT, VERBOSE_OPTION}, description = "Start with Verbose logging")
    private boolean verbose = false;

    @CommandLine.Option(names = {"-w", "--watcher"}, description = "Activate the (internal) monitoring tooling")
    private WatcherType watcher = WatcherType.MINIMAL;

    @CommandLine.Option(names = {"-r", "--root"}, description = "Location of the Atbash runtime installation, current directory by default")
    private String rootDirectory = ".";

    @CommandLine.Option(names = {"--deploymentdirectory"}, description = "Deployment directory that is searched for WAR files which needs to be deployed.")
    private File deploymentDirectory;

    @CommandLine.Option(names = {"-n", "--configName"}, description = "Configuration name")
    private String configName = "default";

    @CommandLine.Option(names = { "--port"}, description = "Port number assigned to the process running the Atbash runtime.")
    private int port = -1;  // We can't use 8080 as we don't know if the user specified a value 8080 or if it is the default.

    @CommandLine.Option(names = {LOG_TO_CONSOLE_OPTION}, description = "Does the Runtime logs to the console?")
    private boolean logToConsole = false;

    @CommandLine.Option(names = {"--no-logToFile"}, description = "Does the Runtime logs to the logging file?", negatable = true)
    private boolean logToFile = true;

    @CommandLine.Option(names = {"--logConfiguration"}, description = "Points to the logging configuration properties file.")
    private File logConfigurationFile;

    @CommandLine.Option(names = {"--warmup"}, description = "In warmup mode, runtime exists when application(s) are ready.")
    private boolean warmup = false;

    @CommandLine.Option(names = {"--contextroot"}, description = "The context root for the application. Comma separated list when multiple applications are deployed.")
    private String contextRoot = "";

    @CommandLine.Option(names = {"--stateless"}, description = "In stateless mode, no configuration files are written and logs are places in the temp directory.")
    private boolean stateless = false;

    @CommandLine.Option(names = {"-c", "--configfile"}, description = "Configuration file that needs to be executed after startup and before applications are deployed")
    private File configFile;

    @CommandLine.Option(names = {"--datafile"}, description = "Configuration properties file that are provided to deployer and application")
    private File configDataFile;

    @CommandLine.Parameters(index = "0..*")
    private File[] archives;

    private boolean embeddedMode;

    private boolean jakartaRunner;

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

    // FIXME Review if we don't need this (and just looking at the raw parameters very early on)
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

    /**
     * Returns the value specified on command line or -1 when not specified. Can be used to determine if
     * value is specified.
     * @return
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the port as specified on the commandline or the default '8080' when the user didn't
     * specify a value.
     * @return
     */
    public int getPortWithDefault() {
        return port == -1 ? 8080 : port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isLogToConsole() {
        return logToConsole;
    }

    public void setLogToConsole(boolean logToConsole) {
        this.logToConsole = logToConsole;
    }

    public boolean isLogToFile() {
        return logToFile;
    }

    public void setLogToFile(boolean logToFile) {
        this.logToFile = logToFile;
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

    public String getContextRoot() {
        return contextRoot;
    }

    public void setContextRoot(String contextRoot) {
        this.contextRoot = contextRoot;
    }

    public boolean isStateless() {
        return stateless;
    }

    public void setStateless(boolean stateless) {
        this.stateless = stateless;
    }

    public File[] getArchives() {
        return archives;
    }

    public void setArchives(File[] archives) {
        this.archives = archives;
    }

    public boolean isEmbeddedMode() {
        return embeddedMode;
    }

    public void setEmbeddedMode() {
        this.embeddedMode = true;
    }

    public boolean isJakartaRunner() {
        return jakartaRunner;
    }

    public void setJakartaRunner() {
        this.jakartaRunner = true;
    }

    public File getConfigFile() {
        return configFile;
    }

    public void setConfigFile(File configFile) {
        this.configFile = configFile;
    }

    public File getConfigDataFile() {
        return configDataFile;
    }

    public void setConfigDataFile(File configDataFile) {
        this.configDataFile = configDataFile;
    }

    @Override
    public String toString() {

        return actualCommandLine();
    }

    public String actualCommandLine() {
        StringBuilder sb = new StringBuilder();
        sb.append("--daemon=").append(daemon);
        sb.append(" --profile=").append(profile);
        if (modules != null) {
            sb.append(", --modules=").append(modules);
        }
        sb.append(" --verbose=").append(verbose);
        sb.append(" --watcher=").append(watcher);
        sb.append(" --root=").append(rootDirectory);
        if (deploymentDirectory != null) {
            sb.append(" --deploymentdirectory=").append(deploymentDirectory);
        }
        sb.append(" --configName=").append(configName);
        sb.append(" --logToConsole=").append(logToConsole);
        sb.append(" --logToFile=").append(logToFile);
        sb.append(" --warmup=").append(warmup);
        sb.append(" --stateless=").append(stateless);
        if (contextRoot != null && !contextRoot.isBlank()) {
            sb.append(" --contextRoot=").append(contextRoot);
        }
        if (configFile != null) {
            sb.append(" --configfile=").append(configFile);
        }
        if (configDataFile != null) {
            sb.append(" --datafile=").append(configDataFile);
        }
        if (archives != null) {
            sb.append(' ');
            sb.append(Arrays.stream(archives).map(File::getAbsolutePath).collect(Collectors.joining(" ")));
        }
        return sb.toString();
    }
}
