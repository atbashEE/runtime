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
package be.atbash.runtime.remotecli.command;

import be.atbash.runtime.common.command.data.CommandResponse;
import be.atbash.runtime.config.ConfigurationManager;
import be.atbash.runtime.config.commands.SetCommand;
import be.atbash.runtime.core.module.RuntimeObjectsManager;

import java.util.List;
import java.util.Map;

public class SetRemoteCommand extends SetCommand implements ServerRemoteCommand {

    @Override
    public CommandResponse handleCommand(Map<String, String> options) {
        CommandResponse result = new CommandResponse();

        ConfigurationManager configurationManager = RuntimeObjectsManager.getInstance().getExposedObject(ConfigurationManager.class);
        List<String> errors = configurationManager.setCommand(options.get("").split(","));

        if (!errors.isEmpty()) {
            result.setErrorMessage(String.join(System.lineSeparator(), errors));
        }

        return result;
    }
}
