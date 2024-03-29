/*
 * Copyright 2021-2023 Rudy De Busscher (https://www.atbash.be)
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
import be.atbash.runtime.config.ConfigurationManager;
import be.atbash.runtime.config.module.exception.ProfileNameException;
import be.atbash.runtime.config.module.profile.ProfileManager;
import be.atbash.runtime.config.util.ConfigFileUtil;
import be.atbash.runtime.config.util.ConfigInstanceUtil;
import be.atbash.runtime.core.data.RunData;
import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.config.Config;
import be.atbash.runtime.core.data.config.ConfigHelper;
import be.atbash.runtime.core.data.config.Endpoint;
import be.atbash.runtime.core.data.deployment.info.PersistedDeployments;
import be.atbash.runtime.core.data.exception.AtbashStartupAbortException;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventManager;
import be.atbash.runtime.core.data.module.event.EventPayload;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.core.data.parameter.ConfigurationParameters;
import be.atbash.runtime.core.data.parameter.WatcherType;
import be.atbash.runtime.core.data.profile.Profile;
import be.atbash.runtime.core.data.watcher.WatcherService;
import be.atbash.runtime.core.module.RuntimeObjectsManager;
import be.atbash.runtime.logging.LoggingUtil;
import be.atbash.runtime.logging.mapping.BundleMapping;
import be.atbash.util.resource.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static be.atbash.runtime.core.data.module.event.Events.CONFIGURATION_UPDATE;

public class ConfigModule implements Module<ConfigurationParameters> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigModule.class);

    private ConfigurationParameters parameters;
    private RuntimeConfiguration runtimeConfiguration;  // FIXME Review the usage of the parameters as the JakartaRunner
    // is not all values from this Object

    private Config config;

    private List<Profile> profiles;

    private ConfigurationManager configurationManager;

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
    public List<Class<?>> getRuntimeObjectTypes() {
        return List.of(RuntimeConfiguration.class, PersistedDeployments.class, ConfigurationManager.class);
    }

    @Override
    public <T> T getRuntimeObject(Class<T> exposedObjectType) {
        if (exposedObjectType.equals(RuntimeConfiguration.class)) {
            return (T) runtimeConfiguration;
        }
        if (exposedObjectType.equals(PersistedDeployments.class)) {
            return (T) ConfigUtil.readApplicationDeploymentsData(runtimeConfiguration);
        }
        if (exposedObjectType.equals(ConfigurationManager.class)) {
            return (T) configurationManager;
        }
        return null;
    }

    @Override
    public Class<? extends Sniffer> moduleSniffer() {
        return null;
    }

    @Override
    public void onEvent(EventPayload eventPayload) {
    }


    @Override
    public void run() {
        BundleMapping.getInstance().addMapping(ConfigFileUtil.class.getName(), ConfigurationManager.class.getName());
        BundleMapping.getInstance().addMapping(ConfigInstanceUtil.class.getName(), ConfigurationManager.class.getName());
        BundleMapping.getInstance().addMapping(ConfigUtil.class.getName(), ConfigurationManager.class.getName());

        WatcherService watcherService = RuntimeObjectsManager.getInstance().getExposedObject(WatcherService.class);
        String msg = LoggingUtil.formatMessage(LOGGER, "CONFIG-1001");
        watcherService.logWatcherEvent(Module.CONFIG_MODULE_NAME, msg, false);

        profiles = ConfigUtil.readProfiles(ResourceUtil.CLASSPATH_PREFIX + "profiles.json", false);
        List<Profile> customProfiles = ConfigUtil.readProfiles("/custom-profile.json", true);
        if (!customProfiles.isEmpty()) {
            profiles = customProfiles;
            parameters.setProfile("custom");
            // We need to resolve the message upfront as this goes to the EarlyLog Handler that cannot handle this
            // and modifies it by adding a * in front of it, so it is no longer recognized as key.
            LOGGER.atInfo().log(LoggingUtil.formatMessage(LOGGER, "CONFIG-021"));
        }
        Profile profile = findProfile();
        ProfileManager profileManager = new ProfileManager(parameters, profile);

        ConfigInstance configInstance;
        if (!parameters.isJakartaRunner()) {
            configInstance = new ConfigInstance(parameters.getRootDirectory(), parameters.getConfigName()
                    , parameters.isStateless(), false);
            ConfigInstanceUtil.processConfigInstance(configInstance);

            if (!configInstance.isValid()) {
                throw new AtbashStartupAbortException();
            } else {
                checkAndSetCustomLoggingFile(configInstance);
                ConfigInstanceUtil.storeRuntimeConfig(configInstance);
                ConfigInstanceUtil.storeLoggingConfig(configInstance);
            }

        } else {
            configInstance = new ConfigInstance(null, null,
                    true, false);
            if (parameters.getLogConfigurationFile() != null) {
                configInstance.setLoggingConfigurationFile(parameters.getLogConfigurationFile().getPath());
            }
        }

        config = ConfigUtil.readConfiguration(configInstance);
        updateConfiguration();

        ConfigUtil.writeConfiguration(configInstance, config);

        RuntimeConfiguration.Builder builder;
        if (parameters.isJakartaRunner()) {
            builder = new RuntimeConfiguration.Builder(null, configInstance.getLoggingConfigurationFile(), true);
        } else {
            if (parameters.isStateless()) {
                if (configInstance.getConfigName() != null) {
                    builder = new RuntimeConfiguration.Builder(configInstance.getConfigDirectory(), configInstance.getLoggingConfigurationFile(), true);
                } else {
                    builder = new RuntimeConfiguration.Builder(configInstance.getLoggingConfigurationFile());
                }
            } else {
                builder = new RuntimeConfiguration.Builder(configInstance.getConfigDirectory(), parameters.getConfigName());
            }
        }
        builder.setRequestedModules(profileManager.getRequestedModules());
        builder.setConfig(config);
        runtimeConfiguration = builder.build();

        EventManager.getInstance().publishEvent(CONFIGURATION_UPDATE, runtimeConfiguration);

        if (!parameters.isStateless()) {
            RunData runData = RuntimeObjectsManager.getInstance().getExposedObject(RunData.class);
            runData.registerDeploymentListener(new ArchiveDeploymentStorage(runtimeConfiguration));
        }

        msg = LoggingUtil.formatMessage(LOGGER, "CONFIG-1002");
        watcherService.logWatcherEvent(Module.CONFIG_MODULE_NAME, msg, false);

        configurationManager = new ConfigurationManager(runtimeConfiguration);
    }

    private void checkAndSetCustomLoggingFile(ConfigInstance configInstance) {
        if (parameters.getLogConfigurationFile() == null) {
            // Nothing to do
            return;
        }
        if (!parameters.getLogConfigurationFile().exists()
                || !parameters.getLogConfigurationFile().isFile()
                || !parameters.getLogConfigurationFile().canRead()) {
            LOGGER.atError().addArgument(parameters.getLogConfigurationFile()).log("CONFIG-018");
            throw new AtbashStartupAbortException();
        }
        configInstance.setLoggingConfigurationFile(parameters.getLogConfigurationFile().getAbsolutePath());
    }

    private Profile findProfile() {

        Optional<Profile> profile = profiles.stream()
                .filter(p -> p.getName().equals(parameters.getProfile()))
                .findAny();
        if (profile.isEmpty()) {
            throw new ProfileNameException(parameters.getProfile());
        }
        return profile.get();
    }

    private void updateConfiguration() {
        // LogToConsole and logToFile from command line only effective for current run, configuration file not updated.
        if (parameters.isLogToConsole()) {
            config.getLogging().overruleLogToConsole(true);
        }
        if (!parameters.isLogToFile()) {
            config.getLogging().overruleLogToFile(false);
        }
        if (parameters.getWatcher() == WatcherType.JFR || parameters.getWatcher() == WatcherType.ALL) {
            config.getMonitoring().setFlightRecorder(true);
        }
        if (parameters.getWatcher() == WatcherType.JMX || parameters.getWatcher() == WatcherType.ALL) {
            config.getMonitoring().setJmx(true);
        }
        if (parameters.getWatcher() == WatcherType.OFF) {
            // Does not disable the JMX service on some OpenJDK ones. But no harm.
            System.setProperty("com.sun.management.jmxremote", "false");
        }

        Endpoint httpEndpoint = ConfigHelper.getHttpEndpoint(config);
        if (parameters.getPort() != -1) {
            httpEndpoint.setPort(parameters.getPortWithDefault());
        }
    }
}
