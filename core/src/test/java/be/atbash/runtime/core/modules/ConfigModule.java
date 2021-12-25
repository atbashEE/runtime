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
package be.atbash.runtime.core.modules;

import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.parameter.ConfigurationParameters;

import java.io.File;
import java.util.List;

import static be.atbash.runtime.core.ModuleManagerTest.FAIL_CONFIG_MODULE;
import static org.junit.jupiter.api.Assertions.fail;

public class ConfigModule extends AbstractTestModule<ConfigurationParameters> {

    private ConfigurationParameters parameters;

    @Override
    public String name() {
        return Module.CONFIG_MODULE_NAME;
    }

    @Override
    public String[] dependencies() {
        return new String[0];
    }

    @Override
    public void setConfig(ConfigurationParameters parameters) {
        this.parameters = parameters;
    }

    @Override
    public Specification[] provideSpecifications() {
        return new Specification[0];
    }

    @Override
    public List<Class<?>> getRuntimeObjectTypes() {
        return List.of(RuntimeConfiguration.class);
    }

    @Override
    public <T> T getRuntimeObject(Class<T> exposedObjectType) {
        if (exposedObjectType.equals(RuntimeConfiguration.class)) {

            RuntimeConfiguration.Builder builder = new RuntimeConfiguration.Builder(new File("."), "");
            builder.setRequestedModules(parameters.getModules().split(","));
            return (T) builder.build();
        }
        return null;
    }

    @Override
    public void run() {
        ModulesLogger.addEvent("Start Config Module");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail(e);
        }
        if (System.getProperty(FAIL_CONFIG_MODULE) != null) {
            throw new IllegalStateException("Forced test exception");
        }
        ModulesLogger.addEvent("End Config Module");
    }
}
