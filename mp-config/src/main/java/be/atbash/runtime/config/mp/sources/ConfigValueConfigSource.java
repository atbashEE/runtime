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

import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Extends the original {@link ConfigSource} to expose methods that return a {@link ConfigValue}. The
 * {@link ConfigValue} allows to retrieve additional metadata associated with the configuration resolution.
 * <p>
 * Based on SmallRye Config.
 */
public interface ConfigValueConfigSource extends ConfigSource {
    /**
     * Return the {@link ConfigValue} for the specified property in this configuration source.
     *
     * @param propertyName the property name
     * @return the ConfigValue, or {@code null} if the property is not present
     */
    ConfigValue getConfigValue(String propertyName);

    /**
     * Return the value for the specified property in this configuration source.
     * <p>
     * <p>
     * This wraps the original {@link ConfigValue} returned by {@link ConfigValueConfigSource#getConfigValue(String)}
     * and unwraps the property value contained {@link ConfigValue}. If the {@link ConfigValue} is null the unwrapped
     * value and return is also null.
     *
     * @param propertyName the property name
     * @return the property value, or {@code null} if the property is not present
     */
    @Override
    default String getValue(String propertyName) {
        ConfigValue value = getConfigValue(propertyName);
        return value != null ? value.getValue() : null;
    }
}
