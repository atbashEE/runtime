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
package be.atbash.runtime.data.microstream.cdi.spi;

import be.atbash.runtime.data.microstream.InstanceStorer;
import be.atbash.runtime.data.microstream.cdi.Storage;
import be.atbash.runtime.data.microstream.cdi.StorageBean;
import be.atbash.runtime.data.microstream.cdi.StoreInterceptor;
import be.atbash.runtime.data.microstream.dirty.DirtyInstanceCollector;
import be.atbash.runtime.data.microstream.dirty.DirtyMarkerImpl;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.*;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class MicroStreamExtension implements Extension {

    private static final Logger LOGGER = Logger.getLogger(MicroStreamExtension.class.getName());

    private final Set<Class<?>> storageRoot = new HashSet<>();

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery event, BeanManager beanManager) {

        addAnnotatedType(event, beanManager, StorageManagerProducer.class);
        addAnnotatedType(event, beanManager, DirtyInstanceCollector.class);
        addAnnotatedType(event, beanManager, DirtyMarkerImpl.class);
        addAnnotatedType(event, beanManager, StoreInterceptor.class);
        addAnnotatedType(event, beanManager, InstanceStorer.class);

    }

    <T> void findRoot(@Observes @WithAnnotations({Storage.class}) ProcessAnnotatedType<T> target) {
        AnnotatedType<T> annotatedType = target.getAnnotatedType();
        if (annotatedType.isAnnotationPresent(Storage.class)) {
            Class<T> javaClass = target.getAnnotatedType().getJavaClass();
            storageRoot.add(javaClass);
            LOGGER.info("New class found annotated with @Storage is " + javaClass);
            target.veto();  // We have a special creation, see StorageBean.
        }
    }

    void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {
        LOGGER.info(String.format("Processing MicroStreamExtension:  %d found", storageRoot.size()));
        if (storageRoot.size() > 1) {
            throw new IllegalStateException(
                    "In the application must have only a class with the Storage annotation, classes: "
                            + storageRoot);
        }

        if (!storageRoot.isEmpty()) {

            StorageBean<?> bean = new StorageBean<>(storageRoot.iterator().next());
            afterBeanDiscovery.addBean(bean);
        }
    }

    void afterTypeDiscovery(@Observes AfterTypeDiscovery afterBeanDiscovery, BeanManager beanManager) {

        if (!storageRoot.isEmpty()) {
            // Interceptor can be added in this afterTypeDiscovery.
            afterBeanDiscovery.getInterceptors().add(StoreInterceptor.class);
        }
    }

    void addAnnotatedType(BeforeBeanDiscovery event, BeanManager beanManager, Class<?> type) {
        String id = "MicroStream" + type.getSimpleName();
        event.addAnnotatedType(beanManager.createAnnotatedType(type), id);
    }

}
