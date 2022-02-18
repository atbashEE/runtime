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

public class EventPayload {

    private final String eventCode;
    private final Object payload;

    public EventPayload(String eventCode, Object payload) {
        this.eventCode = eventCode;
        this.payload = payload;
    }

    public String getEventCode() {
        return eventCode;
    }

    public <T> T getPayload() {
        return (T) payload;
    }
}
