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
package be.atbash.runtime.logging.handler.rotation;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RotationTimerUtil {

    private ScheduledThreadPoolExecutor executor;
    private ScheduledFuture<?> scheduledFuture;
    private final Runnable task;
    private long minutes;

    public RotationTimerUtil(Runnable task, long minutes) {
        executor = new ScheduledThreadPoolExecutor(1);
        executor.setRemoveOnCancelPolicy(true);
        this.task = task;
        this.minutes = minutes;
    }

    private void startTimer() {
        if (executor == null) {
            throw new RotationExecutorInvalidException("LOG-102: The executor for the RotationTimerUtil was already discarded by a call to `cancelExecutor`");
        }
        scheduledFuture = executor.schedule(task, minutes, TimeUnit.MINUTES);
    }

    public void restartTimerForDayBasedRotation() {
        cancelTimer();
        minutes = 60 * 24; // 24 hours x 60 minutes
        startTimer();
    }

    public void restartTimer() {
        cancelTimer();
        startTimer();
    }

    public void cancelTimer() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
    }

    public void cancelExecutor() {
        cancelTimer();
        executor.shutdownNow();
        executor = null;
    }

}
