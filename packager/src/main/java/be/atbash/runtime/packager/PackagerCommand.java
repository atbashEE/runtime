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
package be.atbash.runtime.packager;

import be.atbash.runtime.packager.exception.DirectoryCreationException;
import be.atbash.runtime.packager.exception.InvalidArtifactNameException;
import be.atbash.runtime.packager.files.DirectoryCreator;
import be.atbash.runtime.packager.files.TemplateEngine;
import be.atbash.runtime.packager.maven.MavenCreator;
import be.atbash.runtime.packager.model.Module;
import be.atbash.runtime.packager.model.PackagerOptions;
import be.atbash.runtime.packager.util.ModuleUtil;
import picocli.CommandLine;

import javax.lang.model.SourceVersion;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(name = "Packager")
public class PackagerCommand implements Callable<Integer> {

    @CommandLine.Mixin
    private PackagerOptions packagerOptions;

    private DirectoryCreator directoryCreator;

    @Override
    public Integer call() throws Exception {
        directoryCreator = new DirectoryCreator();
        validate();

        directoryCreator.createDirectory(packagerOptions.getTargetDirectory());

        MavenCreator.createMavenFiles(packagerOptions);

        TemplateEngine templateEngine = new TemplateEngine();

        Map<String, String> variables = new HashMap<>();
        variables.put("artifactId", packagerOptions.getArtifactId());
        String moduleNames = packagerOptions.getRequestedModules().stream()
                .filter(m -> !m.isRequired())  // No need to add the required module names
                .map(m -> String.format("\"%s\"", m.getName()))
                .collect(Collectors.joining(", "));
        variables.put("moduleNames", moduleNames);

        String assemblyDirectory = packagerOptions.getTargetDirectory() + "/src/assembly";
        directoryCreator.createDirectory(assemblyDirectory);
        templateEngine.processTemplateFile(assemblyDirectory, "assembly.xml", "assembly.xml", variables);

        String resourcesDirectory = packagerOptions.getTargetDirectory() + "/src/main/resources";
        directoryCreator.createDirectory(resourcesDirectory);
        templateEngine.processTemplateFile(resourcesDirectory, "custom-profile.json", "custom-profile.json", variables);


        return 0;
    }

    private void validate() {
        if (directoryCreator.existsDirectory(packagerOptions.getTargetDirectory())) {
            throw new DirectoryCreationException(String.format("Directory already exists %s", packagerOptions.getTargetDirectory()));
        }

        String name = packagerOptions.getArtifactId();
        if (!SourceVersion.isIdentifier(name) || SourceVersion.isKeyword(name)) {
            throw new InvalidArtifactNameException(String.format("The 'artifactId' should be a valid Java Name : %s", name));
        }


        // The following call throws an exception when modules not correct
        Set<Module> modules = ModuleUtil.determineModules(packagerOptions.getModules(), ModuleUtil.loadModuleInformation());
        packagerOptions.setRequestedModules(modules);

    }

    public PackagerOptions getPackagerOptions() {
        return packagerOptions;
    }
}
