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
package be.atbash.runtime.core;

import be.atbash.runtime.core.data.RunData;
import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventPayload;
import be.atbash.runtime.core.data.module.event.Events;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.core.data.parameter.WatcherType;
import be.atbash.runtime.core.data.watcher.WatcherService;

import java.util.List;

import static be.atbash.runtime.core.data.module.event.Events.CONFIGURATION_UPDATE;

/**
 * This is a 'pseudo module' so that every other part of the runtime can access some basic information
 * on what is running at the moment.
 */
public class CoreModule implements Module<WatcherType> {

    private RunData runData;
    private WatcherType watcherType;
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

    public void setConfig(WatcherType watcherType) {
        this.watcherType = watcherType;
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
        // The run of the module only requires that we have an empty instance
        // of this instance that can be retrieved.
        runData = new RunData();
        watcherService = new WatcherService(watcherType);
    }
}
