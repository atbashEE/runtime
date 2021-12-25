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
package be.atbash.runtime.command;

import be.atbash.runtime.common.command.*;
import be.atbash.runtime.core.data.exception.AtbashStartupAbortException;
import be.atbash.runtime.core.data.parameter.ConfigurationParameters;
import be.atbash.runtime.core.module.ModuleManager;
import picocli.CommandLine;

@CommandLine.Command(name = "")
public class RuntimeCommand extends AbstractAtbashCommand {

    @CommandLine.Mixin
    private ConfigurationParameters configurationParameters;

    @Override
    public Integer call() throws Exception {

        ModuleManager manager = ModuleManager.initModuleManager(configurationParameters);
        if (!manager.startModules()) {
            throw new AtbashStartupAbortException();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(manager::stopModules));
        return 0;  //Throws exception when start failed.
    }

    public ConfigurationParameters getConfigurationParameters() {
        return configurationParameters;
    }

    @Override
    public String toString() {
        return configurationParameters.toString();
    }
}
