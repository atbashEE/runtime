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
package be.atbash.runtime.core.data.deployment;

import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.WebAppClassLoader;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.core.data.util.StringUtil;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ArchiveDeployment extends AbstractDeployment {

    private File archiveFile;

    private File deploymentLocation;

    // This is about preparation.
    private ArchiveContent archiveContent;
    private WebAppClassLoader classLoader;
    private Set<Specification> specifications;
    private List<Sniffer> sniffers;

    public ArchiveDeployment(File archiveFile) {
        this(archiveFile, StringUtil.determineDeploymentName(archiveFile));
    }

    public ArchiveDeployment(File archiveFile, String deploymentName) {
        super(deploymentName, null, new HashMap<>());
        this.archiveFile = archiveFile;
        deploymentPhase = DeploymentPhase.VERIFIED;
    }

    public ArchiveDeployment(String deploymentLocation, String deploymentName, Set<Specification> specifications,
                             List<Sniffer> sniffers, String contextRoot, Map<String, String> deploymentData) {
        super(deploymentName, contextRoot, deploymentData);
        this.deploymentLocation = new File(deploymentLocation);
        this.specifications = specifications;
        this.sniffers = sniffers;
    }

    public File getArchiveFile() {
        return archiveFile;
    }

    public File getDeploymentLocation() {
        return deploymentLocation;
    }

    public void setDeploymentLocation(File deploymentLocation) {
        this.deploymentLocation = deploymentLocation;
        if (deploymentLocation != null) {
            this.deploymentPhase = DeploymentPhase.VERIFIED;
        }
        // When setting DeploymentLocation it is assumed that it is a verified
        // it is a valid, expanded WAR
    }

    public void setArchiveContent(ArchiveContent archiveContent) {
        this.archiveContent = archiveContent;
    }

    public ArchiveContent getArchiveContent() {
        return archiveContent;
    }

    public void setClassLoader(WebAppClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public WebAppClassLoader getClassLoader() {
        return classLoader;
    }

    public void setSpecifications(Set<Specification> specifications) {
        this.specifications = specifications;
    }

    public Set<Specification> getSpecifications() {
        return specifications;
    }

    public void setSniffers(List<Sniffer> sniffers) {
        this.sniffers = sniffers;
    }

    public List<Sniffer> getSniffers() {
        return sniffers;
    }

}
