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
package be.atbash.runtime.packager.util;

import be.atbash.json.JSONValue;
import be.atbash.json.TypeReference;
import be.atbash.runtime.core.data.exception.UnexpectedException;
import be.atbash.runtime.core.data.util.ResourceReader;
import be.atbash.runtime.packager.exception.UnknownModuleException;
import be.atbash.runtime.packager.model.Module;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public final class ModuleUtil {

    private ModuleUtil() {
    }

    public static List<Module> loadModuleInformation() {
        String content;
        try {
            content = ResourceReader.readResource("/modules.json");
        } catch (IOException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }

        return JSONValue.parse(content,
                new TypeReference<>() {
                });

    }

    public static Set<Module> determineModules(String moduleParameter, List<Module> allModules) {

        List<String> unknownModuleNames = new ArrayList<>();

        Set<Module> modules = Arrays.stream(moduleParameter.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank() && !"null".equals(s))
                .map(s -> findModule(s, allModules, unknownModuleNames))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (!unknownModuleNames.isEmpty()) {
            throw new UnknownModuleException(String.format("Unknown module(s) : %s", String.join(", ", unknownModuleNames)));
        }

        if (modules.isEmpty()) {
            throw new UnknownModuleException("No module(s) specified");
        }

        int size;
        do {
            size = modules.size();

            Set<Module> dependencyModules = modules.stream().map(Module::getDependencies)
                    .flatMap(Arrays::stream)
                    .distinct()
                    .map(s -> findModule(s, allModules, null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            modules.addAll(dependencyModules);

        } while (size < modules.size());

        modules.addAll(allModules.stream()
                .filter(Module::isRequired)
                .collect(Collectors.toSet()));

        return modules;
    }

    private static Module findModule(String moduleName, List<Module> allModules, List<String> unknownModuleNames) {

        Optional<Module> module = allModules.stream()
                .filter(m -> m.getName().equalsIgnoreCase(moduleName))
                .findAny();
        if (module.isEmpty() && unknownModuleNames != null) {
            unknownModuleNames.add(moduleName);
        }
        return module.orElse(null);
    }
}
