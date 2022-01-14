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
package be.atbash.runtime.core.data.module.event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EventManager {

    private static final EventManager INSTANCE = new EventManager();

    private List<ModuleEventListener> listeners = new CopyOnWriteArrayList<>();

    private EventManager() {
    }

    public void registerListener(ModuleEventListener listener) {
        listeners.add(listener); // Fixme check for doubles?
    }

    public void unregisterListener(ModuleEventListener listener) {
        listeners.remove(listener);
    }

    public void publishEvent(String eventCode, Object payload) {
        EventPayload eventPayload = new EventPayload(eventCode, payload);
        ArrayList<ModuleEventListener> currentListeners = new ArrayList<>(listeners);
        currentListeners.forEach(listener -> listener.onEvent(eventPayload));
    }

    public static EventManager getInstance() {
        return INSTANCE;
    }
}
