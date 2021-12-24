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

import be.atbash.runtime.core.data.parameter.ConfigurationParameters;
import be.atbash.runtime.core.data.profile.Profile;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileManagerTest {

    @Test
    void getRequestedModules() {
        Profile profile = new Profile();
        profile.setModules(Arrays.asList("module1", "module2", "module3"));
        ConfigurationParameters parameters = new ConfigurationParameters();
        parameters.setModules("module4");
        ProfileManager profileManager = new ProfileManager(parameters, profile);

        String[] requestedModules = profileManager.getRequestedModules();
        assertThat(requestedModules).contains("Logging", "Config", "module4");
    }

    @Test
    void getRequestedModules_add() {
        Profile profile = new Profile();
        profile.setModules(Arrays.asList("module1", "module2", "module3"));
        ConfigurationParameters parameters = new ConfigurationParameters();
        parameters.setModules("+module4");
        ProfileManager profileManager = new ProfileManager(parameters, profile);
        profileManager.getRequestedModules();

        String[] requestedModules = profileManager.getRequestedModules();
        assertThat(requestedModules).contains("Logging", "Config", "module1", "module2", "module3", "module4");

    }

    @Test
    void getRequestedModules_substract() {
        Profile profile = new Profile();
        profile.setModules(Arrays.asList("module1", "module2", "module3"));
        ConfigurationParameters parameters = new ConfigurationParameters();
        parameters.setModules("-module2");
        ProfileManager profileManager = new ProfileManager(parameters, profile);

        String[] requestedModules = profileManager.getRequestedModules();
        assertThat(requestedModules).contains("Logging", "Config", "module1", "module3");

    }

    @Test
    void getRequestedModules_complex() {
        Profile profile = new Profile();
        profile.setModules(Arrays.asList("module1", "module2", "module3"));
        ConfigurationParameters parameters = new ConfigurationParameters();
        parameters.setModules("module5, +module3, +module6, -module2");
        ProfileManager profileManager = new ProfileManager(parameters, profile);

        String[] requestedModules = profileManager.getRequestedModules();
        assertThat(requestedModules).contains("Logging", "Config", "module5", "module3", "module6");

    }

    @Test
    void getRequestedModules_NoModules() {
        Profile profile = new Profile();
        profile.setModules(Arrays.asList("module1", "module2", "module3"));
        ConfigurationParameters parameters = new ConfigurationParameters();

        ProfileManager profileManager = new ProfileManager(parameters, profile);

        String[] requestedModules = profileManager.getRequestedModules();
        assertThat(requestedModules).contains("Logging", "Config", "module1", "module2", "module3");
    }
}