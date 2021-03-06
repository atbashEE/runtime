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


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PersistedDeployments {
    private List<DeploymentMetadata> deployments = new ArrayList<>();

    public List<DeploymentMetadata> getDeployments() {
        return deployments;
    }

    public void setDeployments(List<DeploymentMetadata> deployments) {
        this.deployments = deployments;
    }

    public void addDeployment(DeploymentMetadata metadata) {
        if (findMetadata(metadata).isEmpty()) {
            deployments.add(metadata);
        }
    }

    private Optional<DeploymentMetadata> findMetadata(DeploymentMetadata metadata) {
        return deployments.stream()
                .filter(m -> m.getDeploymentName().equals(metadata.getDeploymentName()))
                .findAny();

    }

    public void removeDeployment(DeploymentMetadata metadata) {
        deployments.remove(metadata);
    }
}
