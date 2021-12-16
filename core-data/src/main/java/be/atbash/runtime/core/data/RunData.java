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
import java.util.Collections;
import java.util.List;

public class RunData {

    private List<String> startedModules;
    private final List<ArchiveDeployment> deployments = new ArrayList<>();
    private final List<ArchiveDeploymentListener> listeners = new ArrayList<>();
    private boolean domainMode;

    public List<String> getStartedModules() {
        return startedModules;
    }

    public void setStartedModules(List<String> startedModules) {
        this.startedModules = Collections.unmodifiableList(startedModules);
    }

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
        // TODO I don't think there is a need for the unregister
        if (!listeners.contains(listeners)) {
            listeners.add(listener);
        }
    }

    public void setDomainMode() {
        domainMode = true;
    }

    public boolean isDomainMode() {
        return domainMode;
    }

    public void undeployed(ArchiveDeployment deployment) {
        deployments.remove(deployment);
        listeners.forEach(listener -> {
            CriticalThreadCount.getInstance().newCriticalThreadStarted();
            new Thread(
                    () -> listener.archiveDeploymentRemoved(deployment)
            ).start();
        });
    }
}
