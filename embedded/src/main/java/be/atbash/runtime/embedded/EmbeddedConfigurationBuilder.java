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
package be.atbash.runtime.embedded;

import be.atbash.runtime.core.data.parameter.ConfigurationParameters;
import be.atbash.runtime.core.data.parameter.WatcherType;
import be.atbash.runtime.core.data.util.ArchiveDeploymentUtil;
import be.atbash.runtime.core.data.util.StringUtil;

import java.io.File;

public class EmbeddedConfigurationBuilder {

    private ConfigurationParameters configurationParameters;


    public EmbeddedConfigurationBuilder(File archive) {
        this(archive, StringUtil.determineDeploymentName(archive));
    }

    public EmbeddedConfigurationBuilder(File archive, String root) {
        if (ArchiveDeploymentUtil.testOnArchive(archive)) {
            createConfigurationWithDefaults();
            configurationParameters.setArchives(new File[]{archive});
            configurationParameters.setContextRoot(root);
        }
    }

    private void createConfigurationWithDefaults() {
        configurationParameters = new ConfigurationParameters();
        configurationParameters.setLogToFile(false);
        configurationParameters.setStateless(true);
        configurationParameters.setWatcher(WatcherType.OFF);

    }

    public EmbeddedConfigurationBuilder withProfile(String profile) {
        configurationParameters.setProfile(profile);
        return this;
    }

    public EmbeddedConfigurationBuilder withModules(String modules) {
        configurationParameters.setModules(modules);
        return this;
    }

    public ConfigurationParameters build() {
        return configurationParameters;
    }
}
