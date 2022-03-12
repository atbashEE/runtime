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
package be.atbash.runtime.config.module;

import be.atbash.json.JSONValue;
import be.atbash.json.TypeReference;
import be.atbash.runtime.config.ConfigInstance;
import be.atbash.runtime.config.RuntimeConfigConstants;
import be.atbash.runtime.config.module.exception.IncorrectFileContentException;
import be.atbash.runtime.config.util.ConfigFileUtil;
import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.config.Config;
import be.atbash.runtime.core.data.config.Modules;
import be.atbash.runtime.core.data.deployment.info.PersistedDeployments;
import be.atbash.runtime.core.data.exception.UnexpectedException;
import be.atbash.runtime.core.data.profile.Profile;
import be.atbash.runtime.core.data.util.ResourceReader;
import be.atbash.util.exception.AtbashException;

import java.io.IOException;
import java.util.List;

final class ConfigUtil {

    private ConfigUtil() {
    }

    public static PersistedDeployments readApplicationDeploymentsData(RuntimeConfiguration runtimeConfiguration) {

        String content = ConfigFileUtil.readDeployedApplicationsContent(runtimeConfiguration);
        try {
            return JSONValue.parse(content, PersistedDeployments.class);
        } catch (AtbashException e) {
            throw new IncorrectFileContentException(RuntimeConfigConstants.APPLICATIONS_FILE, e);
        }

    }

    public static Config readConfiguration(ConfigInstance configInstance) {
        Config config;
        String content = ConfigFileUtil.readConfigurationContent(configInstance.getConfigDirectory(), configInstance.isStateless());

        try {
            config = JSONValue.parse(content, Config.class);
        } catch (AtbashException e) {
            throw new IncorrectFileContentException(RuntimeConfigConstants.CONFIG_FILE, e);
        }
        if (config.getModules() == null) {
            config.setModules(new Modules());
        }
        return config;
    }

    public static List<Profile> readProfiles() {
        String content;
        try {
            content = ResourceReader.readResource("/profiles.json");
        } catch (IOException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }

        return JSONValue.parse(content,
                new TypeReference<>() {
                });
    }

    public static void writeConfiguration(ConfigInstance configInstance, Config config) {
        String content = JSONValue.toJSONString(config);
        ConfigFileUtil.writeConfigurationContent(configInstance.getConfigDirectory(), configInstance.isStateless(), content);
    }

}
