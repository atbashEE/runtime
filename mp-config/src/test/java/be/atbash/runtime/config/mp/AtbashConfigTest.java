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
package be.atbash.runtime.config.mp;

import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.*;

import static be.atbash.runtime.config.mp.converter.Converters.STRING_CONVERTER;

class AtbashConfigTest {

    @Test
    void getValue() {
        AtbashConfigBuilder builder = new AtbashConfigBuilder().withSources(
                buildConfigSource("foo", "bar")
        );
        HashMap<Type, Converter<?>> converters = new HashMap<>();
        converters.put(String.class, STRING_CONVERTER);
        AtbashConfig config = new AtbashConfig(builder, converters);

        String value = config.getValue("foo", String.class);
        Assertions.assertThat(value).isEqualTo("bar");
    }

    @Test
    void getValue_missing() {
        AtbashConfigBuilder builder = new AtbashConfigBuilder().withSources(
                buildConfigSource("foo", "bar")
        );
        HashMap<Type, Converter<?>> converters = new HashMap<>();
        converters.put(String.class, STRING_CONVERTER);
        AtbashConfig config = new AtbashConfig(builder, converters);

        NoSuchElementException thrownException = Assertions.catchThrowableOfType(
                () -> config.getValue("Atbash", String.class)
                , NoSuchElementException.class);

        Assertions.assertThat(thrownException.getMessage()).isEqualTo("MPCONFIG-114: The config property 'Atbash' is required but it could not be found in any config source");
    }

    @Test
    void getValue_asConfigValue() {
        AtbashConfigBuilder builder = new AtbashConfigBuilder().withSources(
                buildConfigSource("foo", "bar")
        );
        HashMap<Type, Converter<?>> converters = new HashMap<>();
        converters.put(String.class, STRING_CONVERTER);
        AtbashConfig config = new AtbashConfig(builder, converters);

        ConfigValue configValue = config.getValue("foo", ConfigValue.class);
        Assertions.assertThat(configValue).isNotNull();
        Assertions.assertThat(configValue.getName()).isEqualTo("foo");
        Assertions.assertThat(configValue.getValue()).isEqualTo("bar");
        Assertions.assertThat(configValue.getRawValue()).isEqualTo("bar");

    }

    @Test
    void getValue_asConfigValue_missing() {
        AtbashConfigBuilder builder = new AtbashConfigBuilder().withSources(
                buildConfigSource("foo", "bar")
        );
        HashMap<Type, Converter<?>> converters = new HashMap<>();
        converters.put(String.class, STRING_CONVERTER);
        AtbashConfig config = new AtbashConfig(builder, converters);


        NoSuchElementException thrownException = Assertions.catchThrowableOfType(
                () -> config.getValue("Atbash", ConfigValue.class)
                , NoSuchElementException.class);

        Assertions.assertThat(thrownException.getMessage()).isEqualTo("MPCONFIG-114: The config property 'Atbash' is required but it could not be found in any config source");

    }

    @Test
    void getConfigValue() {
        AtbashConfigBuilder builder = new AtbashConfigBuilder().withSources(
                buildConfigSource("foo", "bar")
        );
        AtbashConfig config = new AtbashConfig(builder, new HashMap<>());

        ConfigValue configValue = config.getConfigValue("foo");
        Assertions.assertThat(configValue).isNotNull();
        Assertions.assertThat(configValue.getName()).isEqualTo("foo");
        Assertions.assertThat(configValue.getValue()).isEqualTo("bar");
        Assertions.assertThat(configValue.getRawValue()).isEqualTo("bar");
    }

    @Test
    void getConfigValue_missing() {
        AtbashConfigBuilder builder = new AtbashConfigBuilder().withSources(
                buildConfigSource("foo", "bar")
        );
        AtbashConfig config = new AtbashConfig(builder, new HashMap<>());

        ConfigValue configValue = config.getConfigValue("Atbash");
        Assertions.assertThat(configValue).isNotNull();
        Assertions.assertThat(configValue.getName()).isEqualTo("Atbash");
        Assertions.assertThat(configValue.getValue()).isNull();
        Assertions.assertThat(configValue.getRawValue()).isNull();
    }

    @Test
    void getOptionalValue() {
        AtbashConfigBuilder builder = new AtbashConfigBuilder().withSources(
                buildConfigSource("foo", "bar")
        );
        HashMap<Type, Converter<?>> converters = new HashMap<>();
        converters.put(String.class, STRING_CONVERTER);
        AtbashConfig config = new AtbashConfig(builder, converters);

        Optional<String> value = config.getOptionalValue("foo", String.class);
        Assertions.assertThat(value).isPresent();
        Assertions.assertThat(value.get()).isEqualTo("bar");
    }

    @Test
    void getOptionalValue_missing() {
        AtbashConfigBuilder builder = new AtbashConfigBuilder().withSources(
                buildConfigSource("foo", "bar")
        );
        HashMap<Type, Converter<?>> converters = new HashMap<>();
        converters.put(String.class, STRING_CONVERTER);
        AtbashConfig config = new AtbashConfig(builder, converters);

        Optional<String> value = config.getOptionalValue("Atbash", String.class);
        Assertions.assertThat(value).isEmpty();

    }

    @Test
    void getOptionalValue_configValue() {
        AtbashConfigBuilder builder = new AtbashConfigBuilder().withSources(
                buildConfigSource("foo", "bar")
        );
        HashMap<Type, Converter<?>> converters = new HashMap<>();
        converters.put(String.class, STRING_CONVERTER);
        AtbashConfig config = new AtbashConfig(builder, converters);

        Optional<ConfigValue> value = config.getOptionalValue("foo", ConfigValue.class);
        Assertions.assertThat(value).isPresent();
        ConfigValue configValue = value.get();
        Assertions.assertThat(configValue.getName()).isEqualTo("foo");
        Assertions.assertThat(configValue.getValue()).isEqualTo("bar");
        Assertions.assertThat(configValue.getRawValue()).isEqualTo("bar");
    }


    private ConfigSource buildConfigSource(String... values) {
        if (values.length % 2 != 0) {
            throw new IllegalArgumentException("values array must be a multiple of 2");
        }

        Map<String, String> properties = new HashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            properties.put(values[i], values[i + 1]);
        }

        return new ConfigSource() {
            @Override
            public Set<String> getPropertyNames() {
                return properties.keySet();
            }

            @Override
            public String getValue(String propertyName) {
                return properties.get(propertyName);
            }

            @Override
            public String getName() {
                return "test";
            }
        };
    }
}