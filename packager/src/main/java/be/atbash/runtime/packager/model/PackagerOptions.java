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
package be.atbash.runtime.packager.model;

import picocli.CommandLine;

import java.io.File;
import java.util.Set;

public class PackagerOptions {

    @CommandLine.Option(names = {"-r", "--root"}, required = true, description = "Location where the project will be generated to build custom version.")
    private File targetDirectory;

    @CommandLine.Option(names = {"-m", "--modules"}, required = true, description = "Comma separated list of modules that needs to be included.")
    private String modules;

    @CommandLine.Option(names = {"-a", "--artifactId"}, required = true, description = "ArtifactId of the generated project and this is also the final name of the custom runtime.")
    private String artifactId;

    private Set<Module> requestedModules;

    public File getTargetDirectory() {
        return targetDirectory;
    }

    public void setTargetDirectory(File targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    public String getModules() {
        return modules;
    }

    public void setModules(String modules) {
        this.modules = modules;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public Set<Module> getRequestedModules() {
        return requestedModules;
    }

    public void setRequestedModules(Set<Module> requestedModules) {
        this.requestedModules = requestedModules;
    }
}
