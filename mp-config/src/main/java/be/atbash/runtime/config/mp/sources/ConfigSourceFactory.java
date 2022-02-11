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

import java.util.OptionalInt;

/**
 * This {@code ConfigSourceFactory} allows to initialize a {@link ConfigSource}, with access to the current
 * {@link ConfigSourceContext}.
 * <p>
 * <p>
 * The provided {@link ConfigSource} is initialized in priority order and the current {@link ConfigSourceContext} has
 * access to all previous initialized {@code ConfigSources}. This allows the factory to configure the
 * {@link ConfigSource} with all other {@code ConfigSources} available, except for {@code ConfigSources} initialized by
 * another {@code ConfigSourceFactory}.
 * <p>
 * <p>
 * Instances of this interface will be discovered by {@link AtbashConfigBuilder#withSources(ConfigSourceFactory...)}
 * via the {@link java.util.ServiceLoader} mechanism and can be registered by providing a
 * {@code META-INF/services/be.atbash.runtime.config.mp.sources.ConfigSourceFactory} which contains the fully qualified class name of the
 * custom {@link ConfigSourceFactory} implementation.
 * <p>
 * Based on code from SmallRye Config.
 */
public interface ConfigSourceFactory {
    Iterable<ConfigSource> getConfigSources(ConfigSourceContext context);

    /**
     * Returns the factory priority. This is required, because the factory needs to be sorted before doing
     * initialization. Once the factory is initialized, each a {@link ConfigSource} will use its own ordinal to
     * determine the config lookup order.
     *
     * @return the priority value.
     */
    default OptionalInt getPriority() {
        return OptionalInt.empty();
    }
}
