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

import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.deployment.ArchiveDeploymentListener;

import java.util.ArrayList;
import java.util.List;

public class RunData {

    private final List<ArchiveDeployment> deployments = new ArrayList<>();
    private final List<ArchiveDeploymentListener> listeners = new ArrayList<>();

    public void deployed(ArchiveDeployment deployment) {
        deployments.add(deployment);
        listeners.forEach(listener -> {
            CriticalThreadCount.getInstance().newCriticalThreadStarted();
            new Thread(
                    () -> listener.archiveDeploymentDone(deployment)
            ).start();
        });
    }

    public List<ArchiveDeployment> getDeployments() {
        return deployments;
    }

    public void registerDeploymentListener(ArchiveDeploymentListener listener) {
        // TODO I don't think there is a need for the
        if (!listeners.contains(listeners)) {
            listeners.add(listener);
        }
    }
}
