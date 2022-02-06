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
package be.atbash.runtime.config.mp.sources;

import be.atbash.runtime.config.mp.ConfigValueImpl;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 *
 * Create a wrapper for {@link ConfigSource} as a  {@link ConfigValueConfigSource}.
 */
class ConfigValueConfigSourceWrapper implements ConfigValueConfigSource, Serializable {

    private final ConfigSource configSource;

    private ConfigValueConfigSourceWrapper(ConfigSource configSource) {
        this.configSource = configSource;
    }

    @Override
    public ConfigValue getConfigValue(String propertyName) {
        String value = configSource.getValue(propertyName);
        if (value != null) {
            return ConfigValueImpl.builder()
                    .withName(propertyName)
                    .withValue(value)
                    .withRawValue(value)
                    .withConfigSourceName(getName())
                    .withConfigSourceOrdinal(getOrdinal())
                    .build();
        }

        return null;
    }

    @Override
    public Map<String, String> getProperties() {
        return configSource.getProperties();
    }

    @Override
    public String getValue(String propertyName) {
        return configSource.getValue(propertyName);
    }

    @Override
    public Set<String> getPropertyNames() {
        return configSource.getPropertyNames();
    }

    @Override
    public String getName() {
        return configSource.getName();
    }

    @Override
    public int getOrdinal() {
        return configSource.getOrdinal();
    }

    static ConfigValueConfigSource wrap(ConfigSource configSource) {
        if (configSource instanceof ConfigValueConfigSource) {
            return (ConfigValueConfigSource) configSource;
        } else {
            return new ConfigValueConfigSourceWrapper(configSource);
        }
    }
}
