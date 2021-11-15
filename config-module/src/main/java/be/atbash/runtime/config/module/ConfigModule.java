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
package be.atbash.runtime.config.module;

import be.atbash.runtime.config.ConfigInstance;
import be.atbash.runtime.config.ConfigInstanceUtil;
import be.atbash.runtime.config.module.profile.ProfileManager;
import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.config.Config;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventPayload;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.core.data.parameter.ConfigurationParameters;
import be.atbash.runtime.core.data.util.ResourceReader;
import be.atbash.runtime.monitor.core.Monitoring;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ConfigModule implements Module<ConfigurationParameters> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigModule.class);

    private ConfigurationParameters parameters;
    private RuntimeConfiguration runtimeConfiguration;

    private ConfigurationInformation configurationInformation;

    private boolean statelessConfigRun;

    private Config config;

    @Override
    public String name() {
        return Module.CONFIG_MODULE_NAME;
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
    public Class<ConfigurationParameters> getModuleConfigClass() {
        return ConfigurationParameters.class;
    }

    @Override
    public void setConfig(ConfigurationParameters parameters) {
        this.parameters = parameters;
    }

    @Override
    public List<Class<?>> getExposedTypes() {
        return List.of(RuntimeConfiguration.class);
    }

    @Override
    public <T> T getExposedObject(Class<T> exposedObjectType) {
        if (exposedObjectType.equals(RuntimeConfiguration.class)) {
            return (T) runtimeConfiguration;
        }
        return null;
    }

    @Override
    public Sniffer moduleSniffer() {
        return null;
    }

    @Override
    public void onEvent(EventPayload eventPayload) {
    }


    @Override
    public void run() {
        Monitoring.logMonitorEvent(Module.CONFIG_MODULE_NAME, "Module startup");

        configurationInformation = new ConfigurationInformation();

        // FIXME readOnlyPossible
        ConfigInstance configInstance = new ConfigInstance(parameters.getRootDirectory(), parameters.getConfigName(), true, false);
        ConfigInstanceUtil.processConfigInstance(configInstance);

        // FIXME handle
        if (!configInstance.isValid()) {
            // FIXME
        } else {
            ConfigInstanceUtil.storeRuntimeConfig(configInstance);
            ConfigInstanceUtil.storeLoggingConfig(configInstance);
        }


        ProfileManager profileManager = new ProfileManager(parameters);
        readConfiguration(configInstance);
        overruleConfiguration();

        RuntimeConfiguration.Builder builder = new RuntimeConfiguration.Builder(configInstance.getConfigDirectory(), parameters.getConfigName());
        builder.setRequestedModules(profileManager.getRequestedModules());
        builder.setConfig(config);
        runtimeConfiguration = builder.build();

        Monitoring.logMonitorEvent(Module.CONFIG_MODULE_NAME, "Module ready");
    }

    private void overruleConfiguration() {
        // FIXME Check logic if this is OK.
        if (parameters.isLogToConsole()) {
            config.getLogging().setLogToConsole(true);
        }
    }

    private void readConfiguration(ConfigInstance configInstance) {
        String content = readConfigurationContent(configInstance);
        ObjectMapper mapper = new ObjectMapper();
        try {
            config =  mapper.readValue(content, Config.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            // FIXME
        }
    }

    private String readConfigurationContent(ConfigInstance configInstance) {
        File configFile = new File(configInstance.getConfigDirectory(), "config.json");
        try {
            return Files.readString(configFile.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            // FIXME
        }
        return null;
    }


    private String readProfileJson() throws IOException {
        InputStream profilesJSONStream = ConfigModule.class.getResourceAsStream("/profiles.json");
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int length; (length = profilesJSONStream.read(buffer)) != -1; ) {
            result.write(buffer, 0, length);
        }
        profilesJSONStream.close();
        return result.toString(StandardCharsets.UTF_8.name());
    }


}