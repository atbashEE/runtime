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
package be.atbash.runtime.data.microstream.cdi;


import be.atbash.runtime.data.microstream.cdi.spi.AbstractBean;
import be.atbash.runtime.data.microstream.exception.StorageTypeException;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import one.microstream.reflect.XReflect;
import one.microstream.storage.types.StorageManager;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


/**
 * Storage Discovery Bean to CDI extension to register an entity with {@link Storage}
 * annotation
 */

public class StorageBean<T> extends AbstractBean<T> {
    private final Class<T> type;
    private final Set<Type> types;
    private final Set<Annotation> qualifiers;

    public StorageBean(Class<T> type) {
        this.type = type;
        types = Collections.singleton(type);
        qualifiers = new HashSet<>();
        qualifiers.add(new Default.Literal());
        qualifiers.add(new Any.Literal());
    }

    @Override
    public Class<T> getBeanClass() {
        return type;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T create(CreationalContext<T> context) {
        StorageManager manager = getInstance(StorageManager.class);
        Object root = manager.root();
        T entity;
        if (Objects.isNull(root)) {
            // When storage is empty, create and store the root
            entity = XReflect.defaultInstantiate(type);
            manager.setRoot(entity);
            manager.storeRoot();
        } else {
            if (this.type.isInstance(root)) {
                entity = (T) root;
            } else {
                throw new StorageTypeException(type, root.getClass());
                // The type of @Storage does not match the one that is found in the storage by the Storage manager.

            }
        }
        return entity;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.singleton(Storage.class);
    }

    @Override
    public Set<Type> getTypes() {
        return this.types;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return this.qualifiers;
    }

    @Override
    public String getId() {
        return this.type.getName() + " @Storage";
    }

    @Override
    public String toString() {
        return "StorageBean{"
                +
                "type="
                + this.type
                +
                ", types="
                + this.types
                +
                ", qualifiers="
                + this.qualifiers
                +
                '}';
    }
}
