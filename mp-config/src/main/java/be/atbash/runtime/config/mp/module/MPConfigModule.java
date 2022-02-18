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
package be.atbash.runtime.config.mp.module;

import be.atbash.runtime.config.mp.MPConfigModuleConstant;
import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventPayload;
import be.atbash.runtime.core.data.module.event.Events;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static be.atbash.runtime.config.mp.MPConfigModuleConstant.ENABLED_FORCED;

public class MPConfigModule implements Module<RuntimeConfiguration> {

    private static final Logger LOGGER = Logger.getLogger(MPConfigModule.class.getName());
    public static final String MP_CONFIG_MODULE_NAME = "mp-config";

    public static boolean validationDisable;
    public static boolean configDisabled;

    private RuntimeConfiguration configuration;

    @Override
    public String name() {
        return MP_CONFIG_MODULE_NAME;
    }

    @Override
    public String[] dependencies() {
        return new String[0];
    }

    @Override
    public Specification[] provideSpecifications() {
        return new Specification[0];
    }

    @Override
    public Sniffer moduleSniffer() {
        return new MPConfigSniffer();
    }

    @Override
    public List<Class<?>> getRuntimeObjectTypes() {
        return Collections.emptyList();
    }

    @Override
    public <T> T getRuntimeObject(Class<T> exposedObjectType) {
        return null;
    }

    @Override
    public Class<RuntimeConfiguration> getModuleConfigClass() {
        return RuntimeConfiguration.class;
    }

    @Override
    public void setConfig(RuntimeConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void onEvent(EventPayload eventPayload) {
        if (Events.CONFIGURATION_UPDATE.equals(eventPayload.getEventCode())) {
            configChangedEvent(eventPayload.getPayload());
        }
        if (Events.PRE_DEPLOYMENT.equals(eventPayload.getEventCode())) {
            checkConfigActive(eventPayload.getPayload());
        }
        if (Events.POST_DEPLOYMENT.equals(eventPayload.getEventCode())) {
            configDisabled = false;  // default value.
        }

    }

    private void checkConfigActive(ArchiveDeployment deployment) {
        Map<String, String> moduleConfiguration = configuration.getConfig().getModuleConfiguration(MP_CONFIG_MODULE_NAME);

        String enabledForcedValue = moduleConfiguration.computeIfAbsent(ENABLED_FORCED, k -> "false");
        if (Boolean.parseBoolean(enabledForcedValue)) {
            // Config says that module is always enabled, so don't look at the DeploymentData
            configDisabled = false;
            return;
        }

        String deploymentData = deployment.getDeploymentData(MPConfigModuleConstant.CONFIG_FILES);
        if (deploymentData == null || deploymentData.isBlank()) {
            LOGGER.info(String.format("MPCONFIG-001: MP Config functionality is disabled for application %s", deployment.getDeploymentName()));
            configDisabled = true;
        }
    }


    private void configChangedEvent(RuntimeConfiguration configuration) {
        Map<String, String> moduleConfiguration = configuration.getConfig().getModuleConfiguration(MP_CONFIG_MODULE_NAME);
        String validationDisableConfigValue = moduleConfiguration.computeIfAbsent("validation.disable", k -> "false");
        validationDisable = Boolean.parseBoolean(validationDisableConfigValue);
    }

    @Override
    public void run() {
    }
}