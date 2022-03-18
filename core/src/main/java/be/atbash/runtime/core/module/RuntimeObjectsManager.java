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
package be.atbash.runtime.core.module;

import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.logging.LoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class RuntimeObjectsManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeObjectsManager.class);

    private static final RuntimeObjectsManager INSTANCE = new RuntimeObjectsManager();

    private final Map<Class<?>, Module<?>> runtimeObjectMapping = new HashMap<>();

    void register(Module<?> module) {
        if (module.getRuntimeObjectTypes() == null) {
            return;
        }
        module.getRuntimeObjectTypes().forEach(c -> {
            if (LoggingUtil.isVerbose()) {
                LOGGER.trace(String.format("CORE-1003: Registering instance of '%s' against Module '%s'", c, module.name()));
            }

            runtimeObjectMapping.put(c, module);
        });
    }

    public <T> T getExposedObject(Class<T> exposedObjectType) {
        if (runtimeObjectMapping.containsKey(exposedObjectType)) {
            return runtimeObjectMapping.get(exposedObjectType).getRuntimeObject(exposedObjectType);
        }
        LOGGER.error(String.format("RO-101: Object '%s' not exposed by any Module", exposedObjectType.getName()));

        return null;
    }

    public static RuntimeObjectsManager getInstance() {
        return INSTANCE;
    }
}
