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
package be.atbash.runtime.config;

import be.atbash.json.JSONValue;
import be.atbash.runtime.config.util.ConfigFileUtil;
import be.atbash.runtime.core.data.RuntimeConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Class responsible for executing configuration commands and executing the configuration file after modules startup.
 */
public class ConfigurationManager {

    private final RuntimeConfiguration runtimeConfiguration;

    public ConfigurationManager(RuntimeConfiguration runtimeConfiguration) {
        this.runtimeConfiguration = runtimeConfiguration;
    }

    public List<String> setCommand(String[] options) {
        List<String> result = new ArrayList<>();
        for (String option : options) {
            String[] parts = option.split("=");
            if (parts.length != 2) {
                result.add(String.format("CONFIG-101: Option must be 2 parts separated by =, received '%s'", option));
            } else {
                Optional<String> moduleName = getModuleName(parts[0]);
                if (moduleName.isEmpty()) {
                    result.add(String.format("CONFIG-102: Option key must be '.' separated value, received '%s'", parts[0]));
                } else {
                    String key = parts[0].substring(moduleName.get().length() + 1);
                    runtimeConfiguration.getConfig().getModules().writeConfigValue(moduleName.get(), key, parts[1]);
                }
            }
        }

        if (result.isEmpty()) {
            writeConfigFile();
        }

        return result;
    }

    private void writeConfigFile() {
        String content = JSONValue.toJSONString(runtimeConfiguration.getConfig());
        ConfigFileUtil.writeConfigurationContent(runtimeConfiguration.getConfigDirectory(), runtimeConfiguration.isStateless(), content);
    }

    private Optional<String> getModuleName(String dottedName) {
        int pos = dottedName.indexOf('.');
        if (pos == -1) {
            return Optional.empty();
        } else {
            return Optional.of(dottedName.substring(0, pos));
        }
    }
}
