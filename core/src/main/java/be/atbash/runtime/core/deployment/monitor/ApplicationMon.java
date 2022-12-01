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
package be.atbash.runtime.core.deployment.monitor;

import be.atbash.runtime.core.data.deployment.AbstractDeployment;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;

import java.util.ArrayList;
import java.util.List;

public class ApplicationMon implements ApplicationMonMBean {

    // FIXME This does not expose correctly in JMX (like with JConsole)
    private final List<ApplicationInfo> applications = new ArrayList<>();

    @Override
    public List<ApplicationInfo> getApplications() {
        return applications;
    }

    public void registerApplication(AbstractDeployment deployment) {
        applications.add(ApplicationInfo.createFor(deployment));
    }

    public void unregisterApplication(ArchiveDeployment deployment) {
        applications.remove(ApplicationInfo.createFor(deployment));
    }
}
