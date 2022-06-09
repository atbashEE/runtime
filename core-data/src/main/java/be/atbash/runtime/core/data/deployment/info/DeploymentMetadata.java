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
package be.atbash.runtime.core.data.deployment.info;

import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * data stored to JSON file about deployed application.
 */
public class DeploymentMetadata {

    private String deploymentName;
    private String deploymentLocation;
    private Set<String> specifications;
    private List<String> sniffers;
    private String contextRoot;
    private Map<String, String> deploymentData;

    private String configDataFile;

    // required for Json Processing
    public DeploymentMetadata() {
    }

    public DeploymentMetadata(ArchiveDeployment deployment, RuntimeConfiguration runtimeConfiguration) {
        deploymentName = deployment.getDeploymentName();
        // When undeploying a corrupted Persisted deployment, deploymentLocation is null.
        if (deployment.getDeploymentLocation() != null) {
            String absolutePath = deployment.getDeploymentLocation().getAbsolutePath();
            // Remove config directory location from the path.
            deploymentLocation = absolutePath.substring(runtimeConfiguration.getApplicationDirectory().getAbsolutePath().length());
        }
        specifications = deployment.getSpecifications().stream()
                .map(Specification::name)
                .collect(Collectors.toSet());
        sniffers = deployment.getSniffers().stream()
                .map(s -> s.getClass().getSimpleName())
                .collect(Collectors.toList());
        contextRoot = deployment.getContextRoot();
        deploymentData = deployment.getDeploymentData();
        if (deployment.getConfigDataFile() != null) {
            configDataFile = deployment.getConfigDataFile().getAbsolutePath();
        }
    }

    // setters are for the JSON handling
    public String getDeploymentName() {
        return deploymentName;
    }

    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    public String getDeploymentLocation() {
        return deploymentLocation;
    }

    public void setDeploymentLocation(String deploymentLocation) {
        this.deploymentLocation = deploymentLocation;
    }

    public Set<String> getSpecifications() {
        return specifications;
    }

    public void setSpecifications(Set<String> specifications) {
        this.specifications = specifications;
    }

    public List<String> getSniffers() {
        return sniffers;
    }

    public void setSniffers(List<String> sniffers) {
        this.sniffers = sniffers;
    }

    public String getContextRoot() {
        return contextRoot;
    }

    public void setContextRoot(String contextRoot) {
        this.contextRoot = contextRoot;
    }

    public Map<String, String> getDeploymentData() {
        return deploymentData;
    }

    // required for the JSON de-serialisation.
    public void setDeploymentData(Map<String, String> deploymentData) {
        this.deploymentData = deploymentData;
    }

    public String getConfigDataFile() {
        return configDataFile;
    }

    public void setConfigDataFile(String configDataFile) {
        this.configDataFile = configDataFile;
    }

    // context root is the unique identifier in the system.

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeploymentMetadata)) {
            return false;
        }

        DeploymentMetadata that = (DeploymentMetadata) o;

        return contextRoot.equals(that.contextRoot);
    }

    @Override
    public int hashCode() {
        return contextRoot.hashCode();
    }

}
