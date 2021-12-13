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

import be.atbash.runtime.core.data.exception.UnexpectedException;

import java.util.concurrent.atomic.AtomicInteger;

public final class CriticalThreadCount {

    private static final CriticalThreadCount INSTANCE = new CriticalThreadCount();

    private final AtomicInteger counter = new AtomicInteger(0);

    private CriticalThreadCount() {
    }

    public void newCriticalThreadStarted() {
        counter.incrementAndGet();
    }

    public void criticalThreadFinished() {
        counter.decrementAndGet();
    }

    public void waitForCriticalThreadsToFinish() {
        while (counter.get() > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
            }
        }
    }

    public static CriticalThreadCount getInstance() {
        return INSTANCE;
    }
}
