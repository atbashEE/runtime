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
package be.atbash.runtime.core;

import be.atbash.runtime.core.data.RunData;
import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventPayload;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.core.data.parameter.ConfigurationParameters;
import be.atbash.runtime.core.data.watcher.WatcherService;
import be.atbash.runtime.logging.LoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static be.atbash.runtime.core.data.module.event.Events.CONFIGURATION_UPDATE;

/**
 * This is a 'pseudo module' so that every other part of the runtime can access some basic information
 * on what is running at the moment.
 */
public class CoreModule implements Module<ConfigurationParameters> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreModule.class);

    private RunData runData;
    private ConfigurationParameters configurationParameters;
    private WatcherService watcherService;

    @Override
    public String name() {
        return Module.CORE_MODULE_NAME;
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
        return null;
    }

    public void setConfig(ConfigurationParameters configurationParameters) {
        this.configurationParameters = configurationParameters;
    }

    @Override
    public List<Class<?>> getRuntimeObjectTypes() {
        return List.of(RunData.class, WatcherService.class);
    }

    @Override
    public <T> T getRuntimeObject(Class<T> exposedObjectType) {
        if (exposedObjectType.equals(RunData.class)) {
            return (T) runData;
        }
        if (exposedObjectType.equals(WatcherService.class)) {
            return (T) watcherService;
        }
        return null;
    }

    @Override
    public void onEvent(EventPayload eventPayload) {
        if (CONFIGURATION_UPDATE.equals(eventPayload.getEventCode())) {
            watcherService.reconfigure(eventPayload.getPayload());
        }
    }

    @Override
    public void run() {
        if (LoggingUtil.isVerbose()) {
            LOGGER.trace("CORE-1001: Module startup");
        }
        // The run of the module only requires that we have an empty instance
        // of this instance that can be retrieved.
        runData = new RunData();
        if (configurationParameters.isEmbeddedMode()) {
            runData.setEmbeddedMode();
        }
        watcherService = new WatcherService(configurationParameters.getWatcher());
        if (LoggingUtil.isVerbose()) {
            LOGGER.trace("CORE-1002: Module ready");
        }
    }
}
