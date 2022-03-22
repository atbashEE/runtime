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
package be.atbash.runtime.config.module.profile;

import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.parameter.ConfigurationParameters;
import be.atbash.runtime.core.data.profile.Profile;

import java.util.*;
import java.util.stream.Collectors;

public class ProfileManager {

    private static final List<String> REQUIRED_MODULES = List.of(Module.CORE_MODULE_NAME, Module.LOGGING_MODULE_NAME, Module.CONFIG_MODULE_NAME);
    private static final String MODULE_ACTION_ADD = "Add";
    private static final String MODULE_ACTION_REMOVE = "Remove";
    private static final String MODULE_ACTION_REPLACE = "Replace";

    private final ConfigurationParameters parameters;
    private final Profile profile;

    public ProfileManager(ConfigurationParameters parameters, Profile profile) {

        this.parameters = parameters;
        this.profile = profile;
    }

    public String[] getRequestedModules() {
        // We first start with the profile
        Set<String> result = new HashSet<>(profile.getModules());
        if (parameters.getModules() != null && !parameters.getModules().isBlank()) {
            // There are modules defined, let analyse the input

            Map<String, List<String>> moduleActions = Arrays.stream(parameters.getModules().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank() && !"null".equals(s))
                    .collect(Collectors.groupingBy(this::determineModuleAction, Collectors.mapping(this::determineModuleName, Collectors.toList())));

            if (moduleActions.containsKey(MODULE_ACTION_REPLACE)) {
                result.clear();
                result.addAll(moduleActions.get(MODULE_ACTION_REPLACE));
            }
            if (moduleActions.containsKey(MODULE_ACTION_ADD)) {
                result.addAll(moduleActions.get(MODULE_ACTION_ADD));
            }
            if (moduleActions.containsKey(MODULE_ACTION_REMOVE)) {
                moduleActions.get(MODULE_ACTION_REMOVE).forEach(result::remove);
            }

        }

        // We add the Required modules (like config and logging here) so that they
        // appear in the monitoring part. But they are started separately by the ModuleManager
        result.addAll(REQUIRED_MODULES);

        return result.toArray(new String[]{});
    }

    private String determineModuleName(String moduleDefinition) {
        if (moduleDefinition.startsWith("+") || moduleDefinition.startsWith("-")) {
            return moduleDefinition.substring(1);
        }
        return moduleDefinition;
    }

    private String determineModuleAction(String moduleDefinition) {
        String action;

        switch (moduleDefinition.charAt(0)) {
            case '+':
                action = MODULE_ACTION_ADD;
                break;
            case '-':
                action = MODULE_ACTION_REMOVE;
                break;
            default:
                action = MODULE_ACTION_REPLACE;
                break;
        }
        return action;
    }
}
