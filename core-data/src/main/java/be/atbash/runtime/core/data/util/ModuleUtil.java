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
package be.atbash.runtime.core.data.util;

import be.atbash.runtime.core.data.module.Module;

import java.util.List;

public final class ModuleUtil {

    private ModuleUtil() {
    }

    public static Module<Object> findModule(List<Module> modules, String moduleName) {
        return modules.stream()
                .filter(m -> m.name().equals(moduleName))
                .findAny()
                .orElseThrow(() -> new IllegalStateException(String.format("Can't find module %s", moduleName)));  // FIXME We need to validate the modules names to see if they exist.
    }
}
