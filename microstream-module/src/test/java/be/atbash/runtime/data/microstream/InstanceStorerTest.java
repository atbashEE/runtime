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

import one.microstream.reference.Lazy;
import one.microstream.storage.types.StorageManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InstanceStorerTest {

    @Mock
    private StorageManager managerMock;

    @InjectMocks
    private InstanceStorer instanceStorer;

    @BeforeEach
    public void init() {
        instanceStorer.init();
    }

    @Test
    void queueForProcessing() throws InterruptedException {
        Pojo dirtyInstance = new Pojo();
        InstanceData instanceData = new InstanceData(dirtyInstance, true);
        instanceStorer.queueForProcessing(instanceData);

        Thread.sleep(700L);  // Give it some time to process async
        Mockito.verify(managerMock).store(dirtyInstance);

    }

    @Test
    void queueForProcessing_withLazy_Clear() throws InterruptedException {
        Lazy<String> dirtyInstance = Lazy.Reference("LazyReference");
        InstanceData instanceData = new InstanceData(dirtyInstance, true);
        Assertions.assertThat(dirtyInstance.isLoaded()).isTrue();

        instanceStorer.queueForProcessing(instanceData);

        Thread.sleep(500L);  // Give it some time to process async
        Mockito.verify(managerMock).store(dirtyInstance);
        //Assertions.assertThat(dirtyInstance.isLoaded()).isFalse();
        // We cannot test this since the StorageManager does not save the Lazy and
        // the clear we call actually throws an exception because Lazy is not stored.
        // And the logic about if Lazy is stored is using highly shielded off variables.

    }


    @Test
    void stop() throws InterruptedException {
        Thread.sleep(100L);  // Give it some time to initialize

        String threadName = "InstanceStorer pending changes pump";
        Thread pump = getThreadByName(threadName);
        Assertions.assertThat(pump).isNotNull();
        Assertions.assertThat(pump.getName()).isEqualTo(threadName);

        instanceStorer.stop();
        Thread.sleep(500L);  // Give it some time to process

        pump = getThreadByName(threadName);
        Assertions.assertThat(pump).isNull();
    }

    private Thread getThreadByName(String threadName) {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().equals(threadName)) return t;
        }
        return null;
    }
}