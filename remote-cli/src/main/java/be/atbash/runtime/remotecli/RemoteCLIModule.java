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
package be.atbash.runtime.remotecli;

import be.atbash.runtime.core.data.RunData;
import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventPayload;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.core.module.RuntimeObjectsManager;
import org.eclipse.jetty.server.handler.HandlerCollection;

import java.util.Collections;
import java.util.List;

public class RemoteCLIModule implements Module<Void> {
    @Override
    public String name() {
        return "remote-cli";
    }

    @Override
    public String[] dependencies() {
        return new String[]{"jersey"};
    }

    @Override
    public Specification[] provideSpecifications() {
        return new Specification[0];
    }

    @Override
    public Class<? extends Sniffer> moduleSniffer() {
        return null;
    }

    @Override
    public List<Class<?>> getRuntimeObjectTypes() {
        return Collections.emptyList();
    }

    @Override
    public <T> T getRuntimeObject(Class<T> exposedObjectType) {
        return null;
    }

    @Override
    public void onEvent(EventPayload eventPayload) {

    }

    @Override
    public void run() {
        RunData runData = RuntimeObjectsManager.getInstance().getExposedObject(RunData.class);
        runData.setDomainMode();

        HandlerCollection handlers = RuntimeObjectsManager.getInstance().getExposedObject(HandlerCollection.class);
        handlers.addHandler(new DomainHandler());
    }
}
