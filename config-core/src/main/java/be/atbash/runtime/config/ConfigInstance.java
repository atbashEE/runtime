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

import java.io.File;

/**
 * Represents an 'instance' of the configuration; directory where config
 * root is located and the name of the config.
 */
public class ConfigInstance {

    private final String rootDirectory;
    private String configName;
    private final boolean stateless;
    private final boolean createCommand;
    private File configDirectory;
    private boolean existingConfigDirectory;
    private String loggingConfigurationFile;

    public ConfigInstance(String rootDirectory, String configName, boolean stateless, boolean createCommand) {

        this.rootDirectory = rootDirectory;
        this.configName = configName;
        this.stateless = stateless;
        this.createCommand = createCommand;
    }

    public String getRootDirectory() {
        return rootDirectory;
    }

    public String getConfigName() {
        return configName;
    }

    public boolean isStateless() {
        return stateless;
    }

    public boolean isCreateCommand() {
        return createCommand;
    }

    public void setConfigDirectory(File configDirectory) {
        this.configDirectory = configDirectory;
    }

    public File getConfigDirectory() {
        return configDirectory;
    }

    public void setExistingConfigDirectory(boolean existingConfigDirectory) {
        this.existingConfigDirectory = existingConfigDirectory;
    }

    public boolean isExistingConfigDirectory() {
        return existingConfigDirectory;
    }

    public String getLoggingConfigurationFile() {
        return loggingConfigurationFile;
    }

    public void setLoggingConfigurationFile(String loggingConfigurationFile) {
        this.loggingConfigurationFile = loggingConfigurationFile;
    }

    public boolean isValid() {
        return stateless || configName != null;
    }

    /**
     * Mark the Configuration (root or config name) as invalid.  But isValid() still can return true
     * because we are in readOnly mode.
     */
    public void invalidConfig() {
        configName = null;
    }
}
