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
package be.atbash.runtime.config.module;

import be.atbash.json.JSONValue;
import be.atbash.runtime.config.util.ConfigFileUtil;
import be.atbash.runtime.core.data.CriticalThreadCount;
import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.deployment.ArchiveDeploymentListener;
import be.atbash.runtime.core.data.deployment.info.DeploymentMetadata;
import be.atbash.runtime.core.data.deployment.info.PersistedDeployments;

public class ArchiveDeploymentStorage implements ArchiveDeploymentListener {

    private static final Object LOCK = new Object();
    private final RuntimeConfiguration runtimeConfiguration;

    public ArchiveDeploymentStorage(RuntimeConfiguration runtimeConfiguration) {
        this.runtimeConfiguration = runtimeConfiguration;
    }

    @Override
    public void archiveDeploymentDone(ArchiveDeployment deployment) {
        synchronized (LOCK) {
            PersistedDeployments deployments = ConfigUtil.readApplicationDeploymentsData(runtimeConfiguration);
            deployments.addDeployment(new DeploymentMetadata(deployment, runtimeConfiguration));
            writeApplicationDeploymentsData(deployments);
        }
        CriticalThreadCount.getInstance().criticalThreadFinished();
    }

    @Override
    public void archiveDeploymentRemoved(ArchiveDeployment deployment) {
        synchronized (LOCK) {
            PersistedDeployments deployments = ConfigUtil.readApplicationDeploymentsData(runtimeConfiguration);
            deployments.removeDeployment(new DeploymentMetadata(deployment, runtimeConfiguration));
            writeApplicationDeploymentsData(deployments);
        }
        CriticalThreadCount.getInstance().criticalThreadFinished();
    }

    private void writeApplicationDeploymentsData(PersistedDeployments deployments) {
        String content = JSONValue.toJSONString(deployments);
        ConfigFileUtil.writeDeployedApplicationsContent(runtimeConfiguration, content);

    }
}
