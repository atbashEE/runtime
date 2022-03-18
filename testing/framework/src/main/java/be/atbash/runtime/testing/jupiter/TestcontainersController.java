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
package be.atbash.runtime.testing.jupiter;

import be.atbash.runtime.testing.AtbashContainer;
import be.atbash.runtime.testing.DockerImageContainer;
import be.atbash.runtime.testing.model.ServerAdapterMetaData;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.platform.commons.support.AnnotationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;
import org.testcontainers.junit.jupiter.Container;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Control and manipulate all testContainers.
 */
public class TestcontainersController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestcontainersController.class);

    private final Set<GenericContainer<?>> containers;
    private final Class<?> testClass;
    private Field runtimeContainerField;
    private List<Field> dockerImageContainerFields;

    private AtbashContainer atbashContainer;

    public TestcontainersController(Class<?> testClass) {
        this.testClass = testClass;
        containers = discoverContainers(testClass); // This contains only @Container
        // but method sets runtimeContainerField and dockerImageContainerFields
        // These 2 fields are used in method config() to create the instances and add to containers variable.

    }

    protected Set<GenericContainer<?>> discoverContainers(Class<?> clazz) {

        dockerImageContainerFields = new ArrayList<>();

        Set<GenericContainer<?>> discoveredContainers = new HashSet<>();
        for (Field containerField : AnnotationSupport.findAnnotatedFields(clazz, Container.class)) {
            if (!Modifier.isPublic(containerField.getModifiers())) {
                throw new ExtensionConfigurationException("@Container annotated fields must be public visibility");
            }
            if (!Modifier.isStatic(containerField.getModifiers())) {
                throw new ExtensionConfigurationException("@Container annotated fields must be static");
            }
            boolean isStartable = GenericContainer.class.isAssignableFrom(containerField.getType());
            if (!isStartable) {
                throw new ExtensionConfigurationException("@Container annotated fields must be a subclass of " + GenericContainer.class);
            }
            try {
                boolean generic = true;
                if (containerField.getType().equals(AtbashContainer.class)) {
                    runtimeContainerField = containerField;
                    runtimeContainerField.setAccessible(true);
                    generic = false;
                }
                if (containerField.getType().equals(DockerImageContainer.class)) {
                    dockerImageContainerFields.add(containerField);
                    containerField.setAccessible(true);
                    generic = false;
                }

                if (generic) {
                    // Some other container the developer uses in the test.
                    GenericContainer<?> startableContainer = (GenericContainer<?>) containerField.get(null);
                    startableContainer.setNetwork(Network.SHARED);
                    discoveredContainers.add(startableContainer);


                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                LOGGER.warn("Unable to access field " + containerField, e);
            }
        }
        return discoveredContainers;
    }

    public void config(ServerAdapterMetaData metaData, boolean liveLogging) {
        // Configure the Atbash container.
        // ServerAdapterInstance determine the Docker image which will be used.

        atbashContainer = new AtbashContainer(metaData, liveLogging);

        try {
            runtimeContainerField.set(null, atbashContainer);
            containers.add(atbashContainer);

            for (Field containerField : dockerImageContainerFields) {
                DockerImageContainer dockerImageContainer = new DockerImageContainer(containerField.getName());
                containerField.set(null, dockerImageContainer);
                containers.add(dockerImageContainer);
            }

        } catch (IllegalAccessException e) {
            Assertions.fail(e.getMessage());
        }

    }

    public void start() {

        LOGGER.info("Starting containers in parallel for " + testClass);
        for (GenericContainer<?> c : containers) {
            LOGGER.info("  " + c.getImage());
        }
        long start = System.currentTimeMillis();
        containers.parallelStream().forEach(GenericContainer::start);
        LOGGER.info("All containers started in " + (System.currentTimeMillis() - start) + "ms");
    }


    public void stop() throws IllegalAccessException {
        // Stop all Containers in the AfterAll. Some containers can already be stopped by the AfterEach.
        long start = System.currentTimeMillis();
        containers.parallelStream().forEach(GenericContainer::stop);
        LOGGER.info("All containers stopped in " + (System.currentTimeMillis() - start) + "ms");
        runtimeContainerField.set(null, null);

        for (Field dockerImageContainerField : dockerImageContainerFields) {
            dockerImageContainerField.set(null, null);
        }
    }

    public void waitUntilStopped(int timeoutSeconds) {
        WaitStrategy strategy = new ShutdownWait(atbashContainer).withStartupTimeout(Duration.ofSeconds(timeoutSeconds));
        strategy.waitUntilReady(null);
    }

    private static class ShutdownWait implements WaitStrategy {

        private final AtbashContainer atbashContainer;
        private Duration waitTimeout;

        public ShutdownWait(AtbashContainer atbashContainer) {
            this.atbashContainer = atbashContainer;
        }

        @Override
        public void waitUntilReady(WaitStrategyTarget waitStrategyTarget) {
            CountDownLatch shutdownTimeout = new CountDownLatch(1);
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

            try {
                // Schedule a task to poll for the instance status
                executor.schedule(() -> {
                    Boolean running = atbashContainer.getStatus().getRunning();
                    if (running != null && running) {
                        shutdownTimeout.countDown();
                    }
                }, 200, TimeUnit.MILLISECONDS);


                shutdownTimeout.await(waitTimeout.getSeconds(), TimeUnit.SECONDS);
                // FIXME What should we do if we don't get container up and running within the time frame?
            } catch (InterruptedException e) {
                // re-interrupt for proper clean up
                Thread.currentThread().interrupt();
            } finally {
                executor.shutdown();
            }
        }

        @Override
        public WaitStrategy withStartupTimeout(Duration waitTimeout) {
            this.waitTimeout = waitTimeout;
            return this;
        }
    }
}
