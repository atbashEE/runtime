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
package be.atbash.runtime.remotecli;

import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventPayload;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class RuntimeObjectProvidingModule implements Module<Void> {

    private final Object[] runtimeObjects;

    public RuntimeObjectProvidingModule(Object... runtimeObjects) {
        this.runtimeObjects = runtimeObjects;
    }

    @Override
    public String name() {
        return null;
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

    @Override
    public List<Class<?>> getRuntimeObjectTypes() {
        return null;
    }

    @Override
    public <T> T getRuntimeObject(Class<T> exposedObjectType) {
        Optional<Object> optional = Arrays.stream(runtimeObjects)
                .filter(ro -> exposedObjectType.isAssignableFrom(ro.getClass()))
                .findAny();
        return (T) optional.orElse(null);
    }

    @Override
    public void onEvent(EventPayload eventPayload) {

    }

    @Override
    public void run() {

    }
}

