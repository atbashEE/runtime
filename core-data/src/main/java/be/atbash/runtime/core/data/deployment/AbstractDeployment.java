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
package be.atbash.runtime.core.data.deployment;

import be.atbash.runtime.core.data.module.Module;

import java.io.File;
import java.util.Map;

public class AbstractDeployment {

    private final String deploymentName;

    private Module<?> deploymentModule;
    private final Map<String, String> deploymentData;

    private File configDataFile;

    private Exception deploymentException;

    private boolean applicationReady;

    public AbstractDeployment(String deploymentName, Map<String, String> deploymentData) {
        this.deploymentName = deploymentName;
        this.deploymentData = deploymentData;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public Module<?> getDeploymentModule() {
        return deploymentModule;
    }

    public void setDeploymentModule(Module<?> deploymentModule) {
        this.deploymentModule = deploymentModule;
    }

    public Map<String, String> getDeploymentData() {
        return deploymentData;
    }

    public String getDeploymentData(String key) {
        return deploymentData.get(key);
    }

    public void addDeploymentData(String key, String value) {
        deploymentData.put(key, value);
    }

    public File getConfigDataFile() {
        return configDataFile;
    }

    public void setConfigDataFile(File configDataFile) {
        this.configDataFile = configDataFile;
    }


    public Exception getDeploymentException() {
        return deploymentException;
    }

    public void setDeploymentException(Exception deploymentException) {
        this.deploymentException = deploymentException;
    }

    public boolean hasDeploymentFailed() {
        return getDeploymentException() != null;
    }


    public boolean isApplicationReady() {
        return applicationReady;
    }

    public void setApplicationReady() {
        applicationReady = true;
    }
}
