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

import be.atbash.runtime.config.mp.converter.Converters;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.net.URI;
import java.util.Collections;

/**
 * This {@code AbstractLocationConfigSourceFactory} allows to initialize additional config locations with the
 * configuration {@link ConfigSources#ATBASH_CONFIG_LOCATIONS}. The configuration support multiple
 * locations separated by a comma and each must represent a valid {@link URI}.
 */

public abstract class AbstractLocationConfigSourceFactory extends AbstractLocationConfigSourceLoader
        implements ConfigSourceFactory {

    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        ConfigValue value = context.getValue(ConfigSources.ATBASH_CONFIG_LOCATIONS);
        if (value.getValue() == null) {
            return Collections.emptyList();
        }

        return loadConfigSources(Converters.newArrayConverter(Converters.STRING_CONVERTER, String[].class).convert(value.getValue()),
                value.getSourceOrdinal());
    }
}
