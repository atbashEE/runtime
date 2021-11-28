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
package be.atbash.runtime.core.data;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class CriticalThreadCountTest {

    private static List<String> messages = new ArrayList<>();

    @Test
    void test() {
        new Thread(new CriticalThread("Thread1", 0, 1000)).start();
        new Thread(new CriticalThread("Thread2", 500, 1500)).start();

        CriticalThreadCount.getInstance().waitForCriticalThreadsToFinish();

        Assertions.assertThat(messages).containsExactly("Thread1", "Thread2");
    }

    private static class CriticalThread implements Runnable {

        private String message;
        private long initialWait;
        private long threadWait;

        public CriticalThread(String message, long initialWait, long threadWait) {
            this.message = message;
            this.initialWait = initialWait;
            this.threadWait = threadWait;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(initialWait);

                CriticalThreadCount.getInstance().newCriticalThreadStarted();
                Thread.sleep(threadWait);
                messages.add(message);
                CriticalThreadCount.getInstance().criticalThreadFinished();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}