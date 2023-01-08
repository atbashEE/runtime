/*
 * Copyright 2021-2023 Rudy De Busscher (https://www.atbash.be)
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

import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.deployment.AbstractDeployment;
import be.atbash.runtime.core.data.deployment.ApplicationExecution;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ApplicationInfo {

    private String name;
    private String contextRoot;
    private Set<Specification> specifications;
    private List<String> sniffers;

    private ApplicationInfo(ArchiveDeployment deployment) {
        name = deployment.getDeploymentName();
        contextRoot = deployment.getContextRoot();  // Can never be null
        specifications = deployment.getSpecifications();
        sniffers = deployment.getSniffers().stream().map(s -> s.getClass().getSimpleName()).collect(Collectors.toList());
    }

    private ApplicationInfo(ApplicationExecution deployment) {
        name = deployment.getDeploymentName();
        contextRoot = "/";
        specifications = Set.of(deployment.getDeploymentModule().provideSpecifications());
        sniffers = Collections.emptyList();
    }

    // JSONB
    public ApplicationInfo() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContextRoot() {
        return contextRoot;
    }

    public void setContextRoot(String contextRoot) {
        this.contextRoot = contextRoot;
    }

    public Set<Specification> getSpecifications() {
        return specifications;
    }

    public void setSpecifications(Set<Specification> specifications) {
        this.specifications = specifications;
    }

    public List<String> getSniffers() {
        return sniffers;
    }

    public void setSniffers(List<String> sniffers) {
        this.sniffers = sniffers;
    }

    @Override
    public String toString() {
        return String.format("context root for application %s, detected specifications %s, triggered sniffers %s"
                , contextRoot
                , specifications.stream().map(Enum::name).collect(Collectors.joining(", "))
                , String.join(", ", sniffers));
    }

    @Override
    public boolean equals(Object o) {
        // contextRoot makes this object unique.
        if (this == o) {
            return true;
        }
        if (!(o instanceof ApplicationInfo)) {
            return false;
        }

        ApplicationInfo that = (ApplicationInfo) o;

        return contextRoot.equals(that.contextRoot);
    }

    @Override
    public int hashCode() {
        return contextRoot.hashCode();
    }

    public static ApplicationInfo createFor(AbstractDeployment deployment) {
        if (deployment instanceof ArchiveDeployment) {
            return new ApplicationInfo((ArchiveDeployment) deployment);
        } else {
            return new ApplicationInfo((ApplicationExecution) deployment);
        }
    }
}
