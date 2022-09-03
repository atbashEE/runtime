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

import be.atbash.runtime.packager.exception.UnknownModuleException;
import be.atbash.runtime.packager.model.Module;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class ModuleUtilTest {

    @Test
    void determineModules() {

        List<Module> modules = ModuleUtil.loadModuleInformation();

        List<Module> required = modules.stream().filter(Module::isRequired).collect(Collectors.toList());

        Set<Module> moduleSet = ModuleUtil.determineModules("jetty", modules);

        Assertions.assertThat(moduleSet).hasSize(required.size() + 1);

        required.forEach(moduleSet::remove);
        Assertions.assertThat(moduleSet.iterator().next().getName()).isEqualTo("jetty");
    }

    @Test
    void determineModules_addDependencies() {

        List<Module> modules = ModuleUtil.loadModuleInformation();

        List<Module> required = modules.stream().filter(Module::isRequired).collect(Collectors.toList());

        Set<Module> moduleSet = ModuleUtil.determineModules("jersey", modules);

        Assertions.assertThat(moduleSet).hasSize(required.size() + 2);
        //+2 -> jersey, jetty

        required.forEach(moduleSet::remove);

        List<String> names = moduleSet.stream().map(Module::getName).collect(Collectors.toList());
        Assertions.assertThat(names).containsExactlyInAnyOrderElementsOf(List.of("jersey", "jetty"));
    }

    @Test
    void determineModules_addDependencies2() {

        List<Module> modules = ModuleUtil.loadModuleInformation();

        List<Module> required = modules.stream().filter(Module::isRequired).collect(Collectors.toList());

        Set<Module> moduleSet = ModuleUtil.determineModules("jersey   , jetty", modules);

        Assertions.assertThat(moduleSet).hasSize(required.size() + 2);

        required.forEach(moduleSet::remove);

        List<String> names = moduleSet.stream().map(Module::getName).collect(Collectors.toList());
        Assertions.assertThat(names).containsExactlyInAnyOrderElementsOf(List.of("jersey", "jetty"));
    }

    @Test
    void determineModules_nonExistent() {

        List<Module> modules = ModuleUtil.loadModuleInformation();

        Assertions.assertThatThrownBy(() -> {
                    ModuleUtil.determineModules("x", modules);
                }).isInstanceOf(UnknownModuleException.class)
                .hasMessage("Unknown module(s) : x");

    }

    @Test
    void determineModules_nonExistent2() {

        List<Module> modules = ModuleUtil.loadModuleInformation();

        Assertions.assertThatThrownBy(() -> {
                    ModuleUtil.determineModules("x,jetty,y", modules);
                }).isInstanceOf(UnknownModuleException.class)
                .hasMessage("Unknown module(s) : x, y");

    }

    @Test
    void determineModules_nonExistent3() {

        List<Module> modules = ModuleUtil.loadModuleInformation();

        Assertions.assertThatThrownBy(() -> {
                    ModuleUtil.determineModules(",,", modules);
                }).isInstanceOf(UnknownModuleException.class)
                .hasMessage("No module(s) specified");

    }
}