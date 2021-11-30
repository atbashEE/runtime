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
package be.atbash.runtime.config.module.profile;

import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.parameter.ConfigurationParameters;
import be.atbash.runtime.core.data.profile.Profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProfileManager {

    public static final List<String> REQUIRED_MODULES = List.of(Module.LOGGING_MODULE_NAME, Module.CONFIG_MODULE_NAME);

    private ConfigurationParameters parameters;
    private Profile profile;

    public ProfileManager(ConfigurationParameters parameters, Profile profile) {

        this.parameters = parameters;
        this.profile = profile;
    }

    public String[] getRequestedModules() {
        List<String> result = new ArrayList<>(defineModules());
        // We add the Required modules (like config and logging here) so that they
        // appear in the monitoring part. But they are started separately by the ModuleManager
        result.addAll(REQUIRED_MODULES);
        result.addAll(profile.getModules());

        // Define Other modules;
        // FIXME based on parameters.getModules().
        return result.toArray(new String[]{});
    }

    private List<String> defineModules() {
        // FIXME check Profile.
        if (parameters.getModules() == null || parameters.getModules().isBlank()) {
            return Collections.emptyList();
        }
        return List.of(parameters.getModules().split(","));
    }
}
