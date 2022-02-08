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
package be.atbash.runtime.config.mp.util;

import be.atbash.runtime.core.data.util.ResourceReader;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * utilities and constants for {@link ConfigSource} implementations
 * <p>
 * Based on code from SmallRye Config.
 */
public class ConfigSourceUtil {

    private static final Logger LOGGER = Logger.getLogger(ConfigSourceUtil.class.getName());

    private ConfigSourceUtil() {
    }

    /**
     * convert {@link Properties} to {@link Map}
     *
     * @param properties {@link Properties} object
     * @return {@link Map} object
     */
    @SuppressWarnings("squid:S2445")
    public static Map<String, String> propertiesToMap(Properties properties) {
        Map<String, String> result = new HashMap<>();

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }

        return result;
    }

    public static Map<String, String> urlToMap(URL locationOfProperties) throws IOException {
        Properties properties = new Properties();

        // We do expect an IOException to be thrown when locationOfProperties does not exists.
        String content = ResourceReader.readStringFromURL(locationOfProperties);

        try (Reader reader = new StringReader(content)) {
            properties.load(reader);
        }

        return propertiesToMap(properties);
    }

    /**
     * Get the ordinal value configured within the given map.
     *
     * @param map            the map to query
     * @param defaultOrdinal the ordinal to return if the ordinal key is not specified
     * @return the ordinal value to use
     */
    public static int getOrdinalFromMap(Map<String, String> map, int defaultOrdinal) {
        String ordStr = map.get(ConfigSource.CONFIG_ORDINAL);
        try {
            return ordStr == null ? defaultOrdinal : Integer.parseInt(ordStr);
        } catch (NumberFormatException e) {
            LOGGER.warning(String.format("The property value for '%s' should be an integer but found '%s'", ConfigSource.CONFIG_ORDINAL, ordStr));
            return defaultOrdinal;
        }
    }
}
