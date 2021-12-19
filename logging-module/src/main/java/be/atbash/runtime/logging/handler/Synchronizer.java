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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Synchronize actions between 2 Threads.
 * <p>
 * Usage
 * <p>
 * Thread 1
 * while (!synchronizer.isSignalled()) {
 * //
 * }
 * synchronizer.release();
 * <p>
 * <p>
 * Thread 2
 * <p>
 * synchronizer.raiseSignal(1, TimeUnit.SECONDS);
 * // Either Thread 1 stopped the loop or we waited 1 sec and can interupt the thread 1
 */
public class Synchronizer {

    private ReentrantLock lock = new ReentrantLock();
    private boolean signalled = false;

    public Synchronizer() {
        lock.lock();
    }

    public synchronized boolean isSignalled() {
        return signalled;
    }

    public synchronized void release() {
        lock.unlock();
    }

    public boolean raiseSignal(long timeout, TimeUnit unit) {
        signalled = true;
        try {
            return lock.tryLock(timeout, unit);
        } catch (InterruptedException e) {
            e.printStackTrace();
            // FIXME logging
        }
        return false; // FIXME
    }

}
