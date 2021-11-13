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
package be.atbash.runtime.core.data;

import be.atbash.runtime.core.data.config.Config;

import java.io.File;

public class RuntimeConfiguration {
    private final File configDirectory;
    private final String configName;
    private String[] requestedModules;
    private Config config;

    private RuntimeConfiguration(File configDirectory, String configName) {
        this.configDirectory = configDirectory;
        this.configName = configName;
    }

    public String[] getRequestedModules() {
        return requestedModules;
    }

    public File getConfigDirectory() {
        return configDirectory;
    }

    public File getApplicationDirectory() {
        return new File(configDirectory, "applications");
    }

    public File getLoggingDirectory() {
        return new File(configDirectory, "logs");
    }

    public String getConfigName() {
        return configName;
    }

    public Config getConfig() {
        return config;
    }

    public static class Builder {

        private final RuntimeConfiguration runtimeConfiguration;

        public Builder(File configDirectory, String configName) {
            runtimeConfiguration = new RuntimeConfiguration(configDirectory, configName);
        }

        public Builder setRequestedModules(String[] requestedModules) {
            runtimeConfiguration.requestedModules = requestedModules;
            return this;
        }

        public Builder setConfig(Config config) {
            runtimeConfiguration.config = config;
            return this;
        }

        public RuntimeConfiguration build() {
            return runtimeConfiguration;
        }
    }

}
