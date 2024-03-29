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
package be.atbash.runtime.data.microstream;

import be.atbash.runtime.core.data.util.Synchronizer;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import one.microstream.reference.Lazy;
import one.microstream.storage.types.StorageManager;

import java.util.ConcurrentModificationException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class InstanceStorer {

    @Inject
    private StorageManager manager;

    private final BlockingQueue<InstanceData> pendingChanges = new ArrayBlockingQueue<>(10000);

    private Synchronizer synchronizer;


    @PostConstruct
    public void init() {
        initializePump();
    }

    void initializePump() {
        Thread pump = new Thread(() -> {
            synchronizer = new Synchronizer();
            while (!synchronizer.isSignalled()) {
                InstanceData data = pendingChanges.peek();
                if (data != null) {
                    try {
                        // We peeked, but let us remove it now from the queue.
                        data = pendingChanges.take();
                    } catch (InterruptedException e) {
                        // Re-interrupt the current thread to have proper cleanup.
                        Thread.currentThread().interrupt();
                    }
                    storeChangedWithRetry(data.getDirtyInstance(), data.isClearLazy());
                } else {
                    try {
                        // TODO is there a better alternative for this busy waiting loop
                        Thread.sleep(500L);
                        // let us wait a bit before peeking again so that we can check the Synchronize
                        // Signaled state.
                    } catch (InterruptedException e) {
                        // Re-interrupt the current thread to have proper cleanup.
                        Thread.currentThread().interrupt();
                    }
                }
            }
            synchronizer.release();
        });
        pump.setName("InstanceStorer pending changes pump");
        pump.setDaemon(true);
        pump.start();
    }

    public void storeChangedWithRetry(Object dirtyInstance, boolean clearLazy) {
        try {
            storeChanged(dirtyInstance, clearLazy);
        } catch (ConcurrentModificationException e) {
            // Due to the design of MicroStream, the Persister assumes a single threaded environment due to the usage of Iterator.
            // If the application modifies a collection in the root that is also stored at the same time we have this exception
            // Lets try again and see if it is successful. Under high load this retry might also fail and then @Store and DirtyMarker should not be
            // used.
            storeChanged(dirtyInstance, clearLazy);
        }
    }

    private void storeChanged(Object dirtyInstance, boolean clearLazy) {
        if (dirtyInstance instanceof Lazy) {
            // When a Lazy is marked, the developer probably wants to store the referenced instance in the Lazy.
            Object instance = ((Lazy<?>) dirtyInstance).peek();
            if (instance != null) {
                manager.store(instance);
            }
        }
        manager.store(dirtyInstance);
        if (clearLazy && dirtyInstance instanceof Lazy) {
            ((Lazy<?>) dirtyInstance).clear();
        }
    }

    public void stop() {
        synchronizer.raiseSignal(1, TimeUnit.SECONDS);
    }

    public void queueForProcessing(InstanceData instanceData) {
        pendingChanges.add(instanceData);
    }
}
