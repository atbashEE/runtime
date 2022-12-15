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
package be.atbash.runtime.core.data;

import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.deployment.ArchiveDeploymentListener;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

class RunDataTest {

    @Test
    void isModuleRunning_yes() {
        RunData runData = new RunData();
        runData.setStartedModules(List.of("moduleA", "moduleB"));

        Assertions.assertThat(runData.isModuleRunning("moduleB")).isTrue();
    }

    @Test
    void isModuleRunning_no() {
        RunData runData = new RunData();
        runData.setStartedModules(List.of("moduleA", "moduleB"));

        Assertions.assertThat(runData.isModuleRunning("moduleC")).isFalse();
    }

    @Test
    @Timeout(value = 700, unit = TimeUnit.MILLISECONDS)
        // The testListener has a sleep of 500 ms, so 700 ms should be ok.
    void deployed() throws InterruptedException {
        RunData runData = new RunData();
        Assertions.assertThat(runData.getDeployments()).isEmpty();

        TestListener listener = new TestListener();
        runData.registerDeploymentListener(listener);
        ArchiveDeployment deployment = new ArchiveDeployment(new File("./applications/test.war"));
        deployment.setContextRoot("/test");
        runData.deployed(deployment);

        Thread.sleep(100L);  // Give some time to execute of the listener = separate thread
        Assertions.assertThat(listener.getDeploymentDone()).containsExactly("/test");
        CriticalThreadCount.getInstance().waitForCriticalThreadsToFinish();
    }

    @Test
    @Timeout(value = 700, unit = TimeUnit.MILLISECONDS)
        // The testListener has a sleep of 500 ms, so 700 ms should be ok.
    void undeployed() throws InterruptedException {

        RunData runData = new RunData();
        Assertions.assertThat(runData.getDeployments()).isEmpty();

        TestListener listener = new TestListener();
        runData.registerDeploymentListener(listener);
        ArchiveDeployment deployment = new ArchiveDeployment(new File("./applications/test.war"));
        deployment.setContextRoot("/test");
        runData.undeployed(deployment);

        Thread.sleep(100L);  // Give some time to execute of the listener = separate thread

        Assertions.assertThat(listener.getDeploymentRemoved()).containsExactly("/test");

        CriticalThreadCount.getInstance().waitForCriticalThreadsToFinish();

    }

    @Test
    @Timeout(value = 700, unit = TimeUnit.MILLISECONDS)
        // The testListener has a sleep of 500 ms, so 700 ms should be ok.
    void failedDeployment() throws InterruptedException {
        RunData runData = new RunData();
        Assertions.assertThat(runData.getDeployments()).isEmpty();

        TestListener listener = new TestListener();
        runData.registerDeploymentListener(listener);
        ArchiveDeployment deployment = new ArchiveDeployment(new File("./applications/test.war"));
        deployment.setContextRoot("/test");
        deployment.setDeploymentException(new RuntimeException());
        runData.failedDeployment(deployment);

        Thread.sleep(100L);  // Give some time to execute of the listener = separate thread
        Assertions.assertThat(listener.getDeploymentDone()).isEmpty();
        CriticalThreadCount.getInstance().waitForCriticalThreadsToFinish();

        Assertions.assertThat(runData.getDeployments()).hasSize(1);
    }

    private static class TestListener implements ArchiveDeploymentListener {

        private final List<String> deploymentDone = new ArrayList<>();
        private final List<String> deploymentRemoved = new ArrayList<>();

        @Override
        public void archiveDeploymentDone(ArchiveDeployment deployment) {
            deploymentDone.add(deployment.getContextRoot());
            processTask();
        }

        @Override
        public void archiveDeploymentRemoved(ArchiveDeployment deployment) {
            deploymentRemoved.add(deployment.getContextRoot());

            processTask();
        }

        private static void processTask() {
            // Simulate the work to be done to see if logic around Critical Threads is implemented.
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            CriticalThreadCount.getInstance().criticalThreadFinished();
        }

        public List<String> getDeploymentDone() {
            return deploymentDone;
        }

        public List<String> getDeploymentRemoved() {
            return deploymentRemoved;
        }
    }
}