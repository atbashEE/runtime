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
package be.atbash.runtime.core.data.deployment.info;

import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;

import java.util.List;
import java.util.stream.Collectors;

/**
 * data stored to JSON file about deployed application.
 */
public class DeploymentMetadata {

    private String deploymentName;
    private String deploymentLocation;
    private List<String> specifications;
    private List<String> sniffers;

    // required for Json Processing
    public DeploymentMetadata() {
    }

    public DeploymentMetadata(ArchiveDeployment deployment, RuntimeConfiguration runtimeConfiguration) {
        deploymentName = deployment.getDeploymentName();
        String absolutePath = deployment.getDeploymentLocation().getAbsolutePath();
        // Remove config directory location from the path.
        deploymentLocation = absolutePath.substring(runtimeConfiguration.getApplicationDirectory().getAbsolutePath().length());
        specifications = deployment.getSpecifications().stream()
                .map(Specification::name)
                .collect(Collectors.toList());
        sniffers = deployment.getSniffers().stream()
                .map(s -> s.getClass().getSimpleName())
                .collect(Collectors.toList());
    }

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

    public List<String> getSpecifications() {
        return specifications;
    }

    public void setSpecifications(List<String> specifications) {
        this.specifications = specifications;
    }

    public List<String> getSniffers() {
        return sniffers;
    }

    public void setSniffers(List<String> sniffers) {
        this.sniffers = sniffers;
    }
}
