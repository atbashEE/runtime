/*
 * Copyright 2021-2023 Rudy De Busscher (https://www.atbash.be)
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

import be.atbash.runtime.config.mp.module.MPConfigModule;
import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventPayload;
import be.atbash.runtime.core.data.module.event.Events;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.jersey.JerseyModule;
import be.atbash.runtime.jersey.util.ExtraPackagesUtil;
import jakarta.enterprise.inject.spi.CDI;

import java.util.Collections;
import java.util.List;

import static be.atbash.runtime.config.mp.MPConfigModuleConstant.ENABLED_FORCED;
import static be.atbash.runtime.config.mp.module.MPConfigModule.MP_CONFIG_MODULE_NAME;


public class MicroStreamModule implements Module<RuntimeConfiguration> {

    public static final String MICROSTREAM_MODULE_NAME = "microstream";

    private RuntimeConfiguration configuration;

    @Override
    public String name() {
        return MICROSTREAM_MODULE_NAME;
    }


    @Override
    public String[] dependencies() {
        return new String[]{MPConfigModule.MP_CONFIG_MODULE_NAME, JerseyModule.JERSEY_MODULE_NAME};
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
        if (Events.PRE_DEPLOYMENT.equals(eventPayload.getEventCode())) {
            addJAXRSProviders(eventPayload.getPayload());
        }
    }

    private void addJAXRSProviders(Object payload) {
        ArchiveDeployment deployment = (ArchiveDeployment) payload;

        ExtraPackagesUtil.addPackages(deployment, "be.atbash.runtime.data.microstream.jaxrs");
    }

    @Override
    public Class<RuntimeConfiguration> getModuleConfigClass() {
        return RuntimeConfiguration.class;
    }

    @Override
    public void setConfig(RuntimeConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void run() {
        // We force that MPConfig module is always active .
        configuration.getConfig().getModules().writeConfigValue(MP_CONFIG_MODULE_NAME, ENABLED_FORCED, "true");

    }

    @Override
    public void stop() {
        // FIXME Review order of Stop as we have one case that this module is stopped after the CDI one (CDI.current() already gone)
        InstanceStorer instanceStorer = CDI.current().select(InstanceStorer.class).get();
        instanceStorer.stop();
    }
}
