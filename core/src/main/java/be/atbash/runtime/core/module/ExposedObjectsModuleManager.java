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
package be.atbash.runtime.core.module;

import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.deployment.Deployer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

// FIXME naming should be different I guess.
public class ExposedObjectsModuleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExposedObjectsModuleManager.class);

    private static final ExposedObjectsModuleManager INSTANCE = new ExposedObjectsModuleManager();

    private Map<Class<?>, Module<?>> exposedObjectMapping = new HashMap<>();

    void register(Module<?> module) {
        module.getExposedTypes().forEach(c -> exposedObjectMapping.put(c, module));
    }

    public <T> T getExposedObject(Class<T> exposedObjectType) {
        if (exposedObjectMapping.containsKey(exposedObjectType)) {
            return exposedObjectMapping.get(exposedObjectType).getExposedObject(exposedObjectType);
        } else {
            LOGGER.error(String.format("DE-101: Object '%s' not exposed by any Module", exposedObjectType.getName()));
        }
        return null;
    }

    public static ExposedObjectsModuleManager getInstance() {
        return INSTANCE;
    }
}
