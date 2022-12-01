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
package be.atbash.runtime.jakarta.executable.impl.command;

import be.atbash.runtime.common.command.AbstractAtbashCommand;
import be.atbash.runtime.core.data.exception.AtbashStartupAbortException;
import be.atbash.runtime.core.data.parameter.ConfigurationParameters;
import be.atbash.runtime.core.module.ModuleManager;
import picocli.CommandLine;

@CommandLine.Command(name = "")
public class RuntimeCommand extends AbstractAtbashCommand {

    @CommandLine.Mixin
    private final ConfigurationRunnerParameters configurationRunnerParameters;
    private ConfigurationParameters configurationParameters;

    public RuntimeCommand(ConfigurationRunnerParameters configurationRunnerParameters) {
        this.configurationRunnerParameters = configurationRunnerParameters;
    }

    @Override
    public Integer call() throws Exception {
        convertParameters();
        ModuleManager manager = ModuleManager.initModuleManager(configurationParameters);
        if (!manager.startModules()) {
            throw new AtbashStartupAbortException();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(manager::stopModules));
        return 0;  //Throws exception when start failed.
    }

    private void convertParameters() {
        configurationParameters = new ConfigurationParameters();

        configurationParameters.setArchives(null);
        configurationParameters.setConfigFile(null);
        configurationParameters.setDaemon(false);
        configurationParameters.setConfigName(null);
        configurationParameters.setContextRoot(null);
        configurationParameters.setDeploymentDirectory(null);

        configurationParameters.setProfile("runner");  // Special profile for Jakarta Runner
        configurationParameters.setStateless(true);  // We don't want to store anything
        configurationParameters.setJakartaRunner();  // Important flag
        configurationParameters.setPort(configurationRunnerParameters.getPort());
        configurationParameters.setModules(configurationRunnerParameters.getModules());
        configurationParameters.setVerbose(configurationRunnerParameters.getVerbose());
        configurationParameters.setConfigDataFile(configurationRunnerParameters.getConfigDataFile());
        configurationParameters.setWarmup(configurationRunnerParameters.isWarmup());
        configurationParameters.setWatcher(configurationRunnerParameters.getWatcher());
        configurationParameters.setLogToFile(configurationRunnerParameters.isLogToFile());
        configurationParameters.setLogToConsole(configurationRunnerParameters.isLogToConsole());


    }

    public ConfigurationRunnerParameters getConfigurationRunnerParameters() {
        return configurationRunnerParameters;
    }

    @Override
    public String toString() {
        return configurationRunnerParameters.toString();
    }
}
