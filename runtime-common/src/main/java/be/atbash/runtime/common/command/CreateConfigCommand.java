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
package be.atbash.runtime.common.command;

import be.atbash.runtime.config.ConfigInstance;
import be.atbash.runtime.config.ConfigInstanceUtil;
import be.atbash.runtime.core.data.parameter.ConfigConfigurationParameters;
import picocli.CommandLine;

@CommandLine.Command(name = "create-config")
public class CreateConfigCommand extends AbstractAtbashCommand {

    @CommandLine.Mixin
    private ConfigConfigurationParameters config;

    @Override
    public CommandType getCommandType() {
        return CommandType.CLI;
    }

    @Override
    public Integer call() throws Exception {
        ConfigInstance configInstance = new ConfigInstance(config.getRootDirectory(), config.getConfigName(), false, true);
        ConfigInstanceUtil.processConfigInstance(configInstance);
        if (!configInstance.isValid()) {
            return -1;
        }

        if (configInstance.isExistingConfigDirectory()) {
            System.out.printf("CI-010: The specified root directory '%s' already exist and can't be used.%n", configInstance.getConfigDirectory());
            return -1;
        }

        ConfigInstanceUtil.storeRuntimeConfig(configInstance);
        ConfigInstanceUtil.storeLoggingConfig(configInstance);
        System.out.println("CLI-104: Command create-config executed successfully.");
        return 0;
    }
}
