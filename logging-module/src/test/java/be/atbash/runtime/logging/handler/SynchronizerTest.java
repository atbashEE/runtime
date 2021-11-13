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
package be.atbash.runtime.logging.handler;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

class SynchronizerTest {

    private static final String THREAD_1_START = "Thread1 Start";
    private static final String THREAD_1_WITHIN_LOOP = "Thread1 within Loop";
    private static final String THREAD_1_END_LOOP = "Thread1 End Loop";
    private static final String THREAD_1_END_RUN = "Thread1 End run";
    private static final String THREAD_2_START = "Thread2 Start";
    private static final String THREAD_2_SIGNAL = "Thread2 Signal";
    private static final String THREAD_2_END_RUN = "Thread2 End run";

    private static final List<String> orderEvents = List.of(
            THREAD_1_START
            , THREAD_1_WITHIN_LOOP
            , THREAD_2_START
            , THREAD_2_SIGNAL
            , THREAD_1_END_LOOP
            , THREAD_1_END_RUN
            , THREAD_2_END_RUN);

    private static List<String> events = new CopyOnWriteArrayList<>();
    private static Synchronizer synchronizer;

    @BeforeEach
    public void setup() {
        events.clear();
    }

    @Test
    public void synchronize_normal() {

        Thread thread1 = new Thread(new Thread1(100));
        Thread thread2 = new Thread(new Thread2(200, 300));
        thread1.start();
        thread2.start();

        try {
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //events.forEach(System.out::println);
        List<Integer> positions = defineEventPositions();
        for (int i = 0; i < positions.size()-1; i++) {
            Assertions.assertThat(positions.get(i)).isLessThan(positions.get(i+1));
        }
    }

    private List<Integer> defineEventPositions() {
        List<Integer> result = new ArrayList<>(Collections.nCopies(orderEvents.size(), -1));

        for (int i = 0; i < events.size(); i++) {
            int idx = orderEvents.indexOf(events.get(i));
            if (result.get(idx) == -1) {
                result.set(idx, i);
            }
        }
        return result;
    }

    @Test
    public void synchronize_NoResponseFromSynchronizer() {

        Thread thread1 = new Thread(new Thread1(1000));
        Thread thread2 = new Thread(new Thread2(1000, 200));
        thread1.start();
        thread2.start();

        try {
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<Integer> positions = defineEventPositions();

        Assertions.assertThat(positions.get(0)).isLessThan(positions.get(1));
        // Thread 1 within Loop can happen after the Thread2 start. Not blocking for this test
        //Assertions.assertThat(positions.get(1)).isLessThan(positions.get(2));
        Assertions.assertThat(positions.get(2)).isLessThan(positions.get(3));
        Assertions.assertThat(positions.get(3)).isLessThan(positions.get(6));
        Assertions.assertThat(positions.get(4)).isEqualTo(-1);
        Assertions.assertThat(positions.get(5)).isEqualTo(-1);
    }


    static class Thread1 implements Runnable {

        private long waitTime;

        public Thread1(long waitTime) {
            this.waitTime = waitTime;
        }

        @Override
        public void run() {
            synchronizer = new Synchronizer();
            events.add(THREAD_1_START);
            while (!synchronizer.isSignalled()) {
                events.add(THREAD_1_WITHIN_LOOP);
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            events.add(THREAD_1_END_LOOP);
            synchronizer.release();
            events.add(THREAD_1_END_RUN);
        }
    }


    static class Thread2 implements Runnable {

        private long waitTimeBeforeStart;
        private long waitTimeForRelease;

        public Thread2(long waitTimeBeforeStart, long waitTimeForRelease) {
            this.waitTimeBeforeStart = waitTimeBeforeStart;
            this.waitTimeForRelease = waitTimeForRelease;
        }

        @Override
        public void run() {
            events.add(THREAD_2_START);

            try {
                Thread.sleep(waitTimeBeforeStart);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            events.add(THREAD_2_SIGNAL);
            synchronizer.raiseSignal(waitTimeForRelease, TimeUnit.MILLISECONDS);
            events.add(THREAD_2_END_RUN);
        }
    }
}
