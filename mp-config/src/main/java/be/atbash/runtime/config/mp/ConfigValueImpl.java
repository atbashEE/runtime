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


import org.eclipse.microprofile.config.ConfigValue;

import java.util.Objects;

/**
 * The ConfigValue is a metadata object that holds additional information after the lookup of a configuration.
 * <p>
 * <p>
 * Right now, it is able to hold information like the configuration name, value, the Config Source from where
 * the configuration was loaded, the ordinal of the Config Source and a line number from where the configuration was
 * read if exists.
 * <p>
 * <p>
 * Based on code of SmallRye Config
 */
public class ConfigValueImpl implements ConfigValue {
    private final String name;
    private final String value;
    private final String rawValue;
    private final String profile;
    private final String configSourceName;
    private final int configSourceOrdinal;

    private ConfigValueImpl(ConfigValueBuilder builder) {
        this.name = builder.name;
        this.value = builder.value;
        this.rawValue = builder.rawValue;
        this.profile = builder.profile;
        this.configSourceName = builder.configSourceName;
        this.configSourceOrdinal = builder.configSourceOrdinal;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getRawValue() {
        return rawValue;
    }

    public String getProfile() {
        return profile;
    }

    @Override
    public String getSourceName() {
        return getConfigSourceName();
    }

    @Override
    public int getSourceOrdinal() {
        return getConfigSourceOrdinal();
    }

    public String getConfigSourceName() {
        return configSourceName;
    }


    public int getConfigSourceOrdinal() {
        return configSourceOrdinal;
    }

    public ConfigValue withName(String name) {
        return from().withName(name).build();
    }

    public ConfigValue withValue(String value) {
        return from().withValue(value).build();
    }

    public ConfigValue withProfile(String profile) {
        return from().withProfile(profile).build();
    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ConfigValue that = (ConfigValue) o;
        return name.equals(that.getName()) &&
                value.equals(that.getValue()) &&
                rawValue.equals(that.getRawValue()) &&
                configSourceName.equals(that.getSourceName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value, configSourceName);
    }

    @Override
    public String toString() {
        return "ConfigValue{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", rawValue='" + rawValue + '\'' +
                ", profile='" + profile + '\'' +
                ", configSourceName='" + configSourceName + '\'' +
                ", configSourceOrdinal=" + configSourceOrdinal +
                '}';
    }

    ConfigValueBuilder from() {
        return new ConfigValueBuilder()
                .withName(name)
                .withValue(value)
                .withRawValue(rawValue)
                .withProfile(profile)
                .withConfigSourceName(configSourceName)
                .withConfigSourceOrdinal(configSourceOrdinal);
    }

    public static ConfigValueBuilder builder() {
        return new ConfigValueBuilder();
    }

    public static class ConfigValueBuilder {
        private String name;
        private String value;
        private String rawValue;
        private String profile;
        private String configSourceName;
        private int configSourceOrdinal;

        public ConfigValueBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public ConfigValueBuilder withValue(String value) {
            this.value = value;
            return this;
        }

        public ConfigValueBuilder withRawValue(String rawValue) {
            this.rawValue = rawValue;
            return this;
        }

        public ConfigValueBuilder withProfile(String profile) {
            this.profile = profile;
            return this;
        }

        public ConfigValueBuilder withConfigSourceName(String configSourceName) {
            this.configSourceName = configSourceName;
            return this;
        }

        public ConfigValueBuilder withConfigSourceOrdinal(int configSourceOrdinal) {
            this.configSourceOrdinal = configSourceOrdinal;
            return this;
        }

        public ConfigValue build() {
            return new ConfigValueImpl(this);
        }
    }

}
