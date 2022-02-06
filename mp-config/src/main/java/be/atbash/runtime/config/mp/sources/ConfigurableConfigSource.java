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

import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.*;

/**
 *
 */
public class ConfigurableConfigSource implements ConfigSource {
    private final ConfigSourceFactory factory;

    public ConfigurableConfigSource(ConfigSourceFactory factory) {
        this.factory = factory;
    }

    @Override
    public Map<String, String> getProperties() {
        return new HashMap<>();
    }

    @Override
    public Set<String> getPropertyNames() {
        return new HashSet<>();
    }

    @Override
    public String getValue(final String propertyName) {
        return null;
    }

    @Override
    public String getName() {
        return factory.getClass().getName();
    }

    @Override
    public int getOrdinal() {
        return factory.getPriority().orElse(DEFAULT_ORDINAL);
    }

    ConfigSourceFactory getFactory() {
        return factory;
    }

    public List<ConfigSource> getConfigSources( ConfigSourceContext context) {
        return unwrap(context, new ArrayList<>());
    }

    private List<ConfigSource> unwrap( ConfigSourceContext context,  List<ConfigSource> configSources) {
        for ( ConfigSource configSource : factory.getConfigSources(context)) {
            if (configSource instanceof ConfigurableConfigSource) {
                configSources.addAll(((ConfigurableConfigSource) configSource).getConfigSources(context));
            } else {
                configSources.add(configSource);
            }
        }
        return configSources;
    }
}
