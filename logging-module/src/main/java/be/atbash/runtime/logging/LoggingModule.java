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
package be.atbash.runtime.logging;

import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventPayload;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.core.data.watcher.WatcherService;
import be.atbash.runtime.core.module.RuntimeObjectsManager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class LoggingModule implements Module<RuntimeConfiguration> {

    private static final Logger LOGGER = Logger.getLogger(LoggingModule.class.getName());
    private RuntimeConfiguration configuration;

    @Override
    public String name() {
        return Module.LOGGING_MODULE_NAME;
    }

    @Override
    public String[] dependencies() {
        return new String[0];
    }

    @Override
    public List<Class<?>> getRuntimeObjectTypes() {
        return new ArrayList<>();
    }

    @Override
    public <T> T getRuntimeObject(Class<T> exposedObjectType) {
        return null;
    }

    @Override
    public Specification[] provideSpecifications() {
        return new Specification[0];
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
    public Sniffer moduleSniffer() {
        return null;
    }

    @Override
    public void onEvent(EventPayload eventPayload) {
    }

    @Override
    public void run() {
        WatcherService watcherService = RuntimeObjectsManager.getInstance().getExposedObject(WatcherService.class);
        watcherService.logWatcherEvent(Module.LOGGING_MODULE_NAME, "LOG-101: Module startup");

        LoggingManager.getInstance().configureLogging(configuration);

        LoggingManager.getInstance().removeEarlyLogHandler();

        watcherService.logWatcherEvent(Module.LOGGING_MODULE_NAME, "LOG-102: Module startup");

    }
}
