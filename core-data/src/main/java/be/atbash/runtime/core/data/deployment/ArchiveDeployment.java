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

    // verified means the deploymentLocation points to a valid expanded war
    // or archiveFile is specified
    private boolean verified;
    // There is also isPrepared. Prepared means that ALL preparation is done
    // Deployed means that the application is responding to user requests.
    private boolean deployed;
    private File deploymentLocation;

    private String contextRoot;

    // This is about preparation.
    private ArchiveContent archiveContent;
    private WebAppClassLoader classLoader;
    private Set<Specification> specifications;
    private List<Sniffer> sniffers;

    public ArchiveDeployment(File archiveFile) {
        this(archiveFile, StringUtil.determineDeploymentName(archiveFile));
    }

    public ArchiveDeployment(File archiveFile, String deploymentName) {
        super(deploymentName, new HashMap<>());
        this.archiveFile = archiveFile;
        this.verified = true;
    }

    public ArchiveDeployment(String deploymentLocation, String deploymentName, Set<Specification> specifications,
                             List<Sniffer> sniffers, String contextRoot, Map<String, String> deploymentData) {
        super(deploymentName, deploymentData);
        this.deploymentLocation = new File(deploymentLocation);
        this.specifications = specifications;
        this.sniffers = sniffers;
        this.contextRoot = contextRoot;
        this.verified = false;
    }

    public File getArchiveFile() {
        return archiveFile;
    }

    public boolean isDeployed() {
        return deployed;
    }

    public void setDeployed() {
        deployed = true;
    }

    public boolean isVerified() {
        return verified;
    }

    public boolean isPrepared() {
        return verified && archiveContent != null &&
                classLoader != null &&
                specifications != null &&
                getDeploymentModule() != null &&
                sniffers != null;
    }

    public File getDeploymentLocation() {
        return deploymentLocation;
    }

    public void setDeploymentLocation(File deploymentLocation) {
        this.deploymentLocation = deploymentLocation;
        this.verified = deploymentLocation != null;
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

    public String getContextRoot() {
        return contextRoot == null ? "/" + getDeploymentName() : contextRoot;
    }

    public void setContextRoot(String contextRoot) {
        this.contextRoot = contextRoot.strip();
        if (!this.contextRoot.startsWith("/")) {
            this.contextRoot = "/" + this.contextRoot;
        }
        if (this.contextRoot.endsWith("/")) {
            this.contextRoot = this.contextRoot.substring(0, this.contextRoot.length() - 1);
        }
    }

    // archiveDeployments are identified by context root.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ArchiveDeployment)) {
            return false;
        }

        ArchiveDeployment that = (ArchiveDeployment) o;

        return contextRoot.equals(that.contextRoot);
    }

    @Override
    public int hashCode() {
        return contextRoot.hashCode();
    }
}
