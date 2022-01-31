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
package be.atbash.runtime.config.commands;

import be.atbash.runtime.config.ConfigurationManager;
import be.atbash.runtime.core.module.RuntimeObjectsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(name = "set")
public class SetCommand extends AbstractConfigurationCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetCommand.class);

    @CommandLine.Parameters(index = "0..*")
    private String[] options;

    @Override
    public Integer call() throws Exception {
        ConfigurationManager configurationManager = RuntimeObjectsManager.getInstance().getExposedObject(ConfigurationManager.class);
        List<String> result = configurationManager.setCommand(options);
        result.forEach(LOGGER::error);  // No code needed in front of messages as they already start with a code.
        return result.isEmpty() ? 0 : -1;
    }
}
