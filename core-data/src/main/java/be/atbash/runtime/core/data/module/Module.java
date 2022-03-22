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
package be.atbash.runtime.core.data.module;

import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.module.event.ModuleEventListener;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;

import java.util.List;

public interface Module<C> extends Runnable, ModuleEventListener {

    String LOGGING_MODULE_NAME = "Logging";
    String CONFIG_MODULE_NAME = "Config";
    String CORE_MODULE_NAME = "Core";

    /**
     * The Name of the module
     *
     * @return
     */
    String name();

    /**
     * The modules that need to be started already before this module can start.
     *
     * @return
     */
    String[] dependencies();

    /**
     * Defines the class that is expected to pass to the {@code setConfig()} method
     * before the call ro the run method is performed.
     *
     * @return
     */
    default Class<C> getModuleConfigClass() {
        return null;
    }

    /**
     * Called by the ModuleManager before the Module is started.
     *
     * @param config
     */
    default void setConfig(C config) {
    }

    /**
     * Reports the specifications the Module handles.
     *
     * @return
     */
    Specification[] provideSpecifications();

    /**
     *
     */
    Sniffer moduleSniffer();

    /**
     * Return the List of Object classes that are exposed by the module and can for
     * which an instance can be retrieved from the module by {@code getExposedObject()} method.
     *
     * @return
     */
    List<Class<?>> getRuntimeObjectTypes();

    <T> T getRuntimeObject(Class<T> exposedObjectType);

    default void stop() {}

    default void registerDeployment(ArchiveDeployment archiveDeployment) {}

    default void unregisterDeployment(ArchiveDeployment archiveDeployment) {}
}
