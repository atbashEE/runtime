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
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.core.data.util.StringUtil;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArchiveDeployment {

    private File archiveFile;
    private final String deploymentName;
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
    private List<Specification> specifications;
    private Module<?> deploymentModule;
    private List<Sniffer> sniffers;

    private final Map<String, String> deploymentData;

    public ArchiveDeployment(File archiveFile) {
        this(archiveFile, StringUtil.determineDeploymentName(archiveFile));
    }

    public ArchiveDeployment(File archiveFile, String deploymentName) {
        this.archiveFile = archiveFile;
        this.deploymentName = deploymentName;
        this.verified = true;
        this.deploymentData = new HashMap<>();
    }

    public ArchiveDeployment(String deploymentLocation, String deploymentName, List<Specification> specifications,
                             List<Sniffer> sniffers, String contextRoot, Map<String, String> deploymentData) {
        this.deploymentLocation = new File(deploymentLocation);
        this.deploymentName = deploymentName;
        this.specifications = specifications;
        this.sniffers = sniffers;
        this.contextRoot = contextRoot;
        this.verified = false;
        this.deploymentData = deploymentData;
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
                deploymentModule != null &&
                sniffers != null;
    }

    public String getDeploymentName() {
        return deploymentName;
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

    public void setSpecifications(List<Specification> specifications) {
        this.specifications = specifications;
    }

    public List<Specification> getSpecifications() {
        return specifications;
    }

    public Module<?> getDeploymentModule() {
        return deploymentModule;
    }

    public void setDeploymentModule(Module<?> deploymentModule) {
        this.deploymentModule = deploymentModule;
    }

    public void setSniffers(List<Sniffer> sniffers) {
        this.sniffers = sniffers;
    }

    public List<Sniffer> getSniffers() {
        return sniffers;
    }

    public String getContextRoot() {
        return contextRoot == null ? "/" + deploymentName : contextRoot;
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

    public String getDeploymentData(String key) {
        return deploymentData.get(key);
    }

    public Map<String, String> getDeploymentData() {
        return new HashMap<>(deploymentData);
    }

    public void addDeploymentData(String key, String value) {
        deploymentData.put(key, value);
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
