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


import be.atbash.runtime.data.microstream.config.EmbeddedStorageFoundationCustomizer;
import be.atbash.runtime.data.microstream.config.StorageManagerInitializer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import one.microstream.reflect.ClassLoaderProvider;
import one.microstream.storage.embedded.configuration.types.EmbeddedStorageConfigurationBuilder;
import one.microstream.storage.embedded.types.EmbeddedStorageFoundation;
import one.microstream.storage.embedded.types.EmbeddedStorageManager;
import one.microstream.storage.types.StorageManager;
import org.eclipse.microprofile.config.Config;

import java.util.Map;
import java.util.logging.Logger;


/**
 * Based on code from the MicroStream CDI integration.
 */
@ApplicationScoped
class StorageManagerProducer {
    private static final Logger LOGGER = Logger.getLogger(StorageManagerProducer.class.getName());

    @Inject
    private Config config;

    @Inject
    private MicroStreamExtension storageExtension;

    @Inject
    private Instance<EmbeddedStorageFoundationCustomizer> customizers;

    @Inject
    private Instance<StorageManagerInitializer> initializers;

    @Produces
    @ApplicationScoped
    public StorageManager getStoreManager() {

        Map<String, String> properties = ConfigurationCoreProperties.getProperties(this.config);
        LOGGER.info(
                "Loading default StorageManager loading from MicroProfile Config properties. The keys: "
                        + properties.keySet()
        );

        EmbeddedStorageConfigurationBuilder builder = EmbeddedStorageConfigurationBuilder.New();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            builder.set(entry.getKey(), entry.getValue());
        }

        EmbeddedStorageFoundation<?> embeddedStorageFoundation = builder.createEmbeddedStorageFoundation();

        // We need the WebApp Classloader since the root contains application classes
        // That are not available from Atbash Runtime nor MicroStream.
        embeddedStorageFoundation.onConnectionFoundation(cf -> cf.setClassLoaderProvider(ClassLoaderProvider.New(
                Thread.currentThread().getContextClassLoader())));

        customizers.stream()
                .forEach(customizer -> customizer.customize(embeddedStorageFoundation));

        EmbeddedStorageManager storageManager = embeddedStorageFoundation.start();

        if (!storageExtension.hasStorageRoot())
        {
            // Only execute at this point when no storage root bean has defined with @Storage
            // Initializers are called from StorageBean.create if user has defined @Storage and root is read.
            initializers.stream()
                    .forEach(initializer -> initializer.initialize(storageManager));
        }

        return storageManager;
    }

    public void dispose(@Disposes StorageManager manager) {
        LOGGER.info("Closing the default StorageManager");
        manager.close();
    }
}
