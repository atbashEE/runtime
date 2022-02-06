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

import be.atbash.runtime.config.mp.util.ConfigSourceUtil;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * FIXME this abstract implementation is a problem when ConfigSource is dynamical and content is changed.
 */
public abstract class MapBackedConfigSource extends AbstractConfigSource {

    private final Map<String, String> properties;

    /**
     * Construct a new instance. The config source will use the given default ordinal, and
     * will use the given map as-is (not a copy of it).
     *
     * @param name           the config source name
     * @param propertyMap    the map to use
     * @param defaultOrdinal the default ordinal to use if one is not given in the map
     */
    public MapBackedConfigSource(String name, Map<String, String> propertyMap, int defaultOrdinal) {
        super(name, ConfigSourceUtil.getOrdinalFromMap(propertyMap, defaultOrdinal));
        properties = Collections.unmodifiableMap(propertyMap);
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public Set<String> getPropertyNames() {
        return properties.keySet();
    }

    @Override
    public String getValue(String propertyName) {
        return properties.get(propertyName);
    }
}
