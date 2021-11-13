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
package be.atbash.runtime.core.data.deployment;

import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.WebAppClassLoader;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;

import java.io.File;
import java.util.List;

public class ArchiveDeployment {

    private final File archiveFile;
    private final String deploymentName;
    private boolean deployed;
    private File deploymentLocation;

    private ArchiveContent archiveContent;
    private WebAppClassLoader classLoader;
    private List<Specification> specifications;
    private Module<?> deploymentModule;
    private List<Sniffer> sniffers;

    public ArchiveDeployment(File archiveFile) {
        this(archiveFile, determineDeploymentName(archiveFile));
        deployed = false;
    }

    private static String determineDeploymentName(File archiveFile) {
        String filename = archiveFile.getName();
        if (filename.endsWith(".war")) {
            filename = filename.substring(0, filename.length() - 4);
        }
        return filename;
    }

    public ArchiveDeployment(File archiveFile, String deploymentName) {
        this.archiveFile = archiveFile;
        this.deploymentName = deploymentName;
        this.deployed = true;
    }

    public File getArchiveFile() {
        return archiveFile;
    }

    public boolean isDeployed() {
        return deployed;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public File getDeploymentLocation() {
        return deploymentLocation;
    }

    public void setDeploymentLocation(File deploymentLocation) {
        this.deploymentLocation = deploymentLocation;
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
        // FIXME make this configurable.
        return "/" + deploymentName;
    }
}
