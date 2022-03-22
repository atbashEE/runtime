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
package be.atbash.runtime.core.data;

import be.atbash.runtime.core.data.config.Config;

import java.io.File;

public class RuntimeConfiguration {
    private File configDirectory; // null when stateless unless config directory already existed
    private String configName;  // null when stateless
    private String loggingConfigurationFile; // for stateless case, otherwise based on configDirectory and configName.
    private String[] requestedModules;
    private Config config;

    // This is for the 'stateful' scenario
    private RuntimeConfiguration(File configDirectory, String configName) {
        this.configDirectory = configDirectory;
        this.configName = configName;
    }

    // This is for the 'stateless' scenario with existing config directory
    private RuntimeConfiguration(File configDirectory, String loggingConfigurationFile, boolean stateless) {
        assert(stateless); // Not sure how we can avoid identical method signatures with different meanings
        this.configDirectory = configDirectory;
        this.loggingConfigurationFile = loggingConfigurationFile;
    }

    public RuntimeConfiguration(String loggingConfigurationFile) {
        this.loggingConfigurationFile = loggingConfigurationFile;
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

    public boolean isStateless() {
        return configName == null;
    }

    public String getLoggingConfigurationFile() {
        return loggingConfigurationFile;
    }

    public static class Builder {

        private final RuntimeConfiguration runtimeConfiguration;

        public Builder(File configDirectory, String configName) {
            runtimeConfiguration = new RuntimeConfiguration(configDirectory, configName);
        }

        public Builder(String loggingConfigurationFile) {
            runtimeConfiguration = new RuntimeConfiguration(loggingConfigurationFile);
        }

        public Builder(File configDirectory, String loggingConfigurationFile, boolean stateless) {
            assert (stateless);  // Not sure how we can avoid identical method signatures with different meanings
            runtimeConfiguration = new RuntimeConfiguration(configDirectory, loggingConfigurationFile, stateless);
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
