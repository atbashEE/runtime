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
package be.atbash.runtime.core.deployment.monitor;

import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;

import java.util.List;
import java.util.stream.Collectors;

public class ApplicationInfo {

    private String name;
    private String contextRoot;
    private List<Specification> specifications;
    private List<String> sniffers;

    public ApplicationInfo(ArchiveDeployment deployment) {
        name = deployment.getDeploymentName();
        contextRoot = deployment.getContextRoot();
        specifications = deployment.getSpecifications();
        sniffers = deployment.getSniffers().stream().map(s -> s.getClass().getSimpleName()).collect(Collectors.toList());
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

    public List<Specification> getSpecifications() {
        return specifications;
    }

    public void setSpecifications(List<Specification> specifications) {
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
}
