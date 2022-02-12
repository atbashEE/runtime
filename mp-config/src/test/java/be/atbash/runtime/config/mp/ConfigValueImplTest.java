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

import nl.jqno.equalsverifier.EqualsVerifier;
import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.ConfigValue;
import org.junit.jupiter.api.Test;

class ConfigValueImplTest {

    @Test
    public void builder_requireName() {
        Assertions.assertThatThrownBy(() ->
                        ConfigValueImpl.builder().build())
                .isInstanceOf(IllegalArgumentException.class);

    }

    @Test
    public void builder_minimal() {
        ConfigValue configValue = ConfigValueImpl.builder().withName("name").build();
        Assertions.assertThat(configValue).isNotNull();
    }

    @Test
    public void builder_withNameIsRequired() {
        ConfigValue configValue = buildConfigValue();

        ConfigValueImpl value = (ConfigValueImpl) configValue;
        Assertions.assertThatThrownBy(() ->
                        value.withName(null))
                .isInstanceOf(IllegalArgumentException.class);

    }

    @Test
    public void builder() {
        ConfigValue configValue = buildConfigValue();

        ConfigValueImpl value = (ConfigValueImpl) configValue;

        Assertions.assertThat(value.getName()).isEqualTo("name");
        Assertions.assertThat(value.getValue()).isEqualTo("value");
        Assertions.assertThat(value.getRawValue()).isEqualTo("rawValue");
        Assertions.assertThat(value.getProfile()).isEqualTo("profile");
        Assertions.assertThat(value.getSourceName()).isEqualTo("source");
        Assertions.assertThat(value.getSourceOrdinal()).isEqualTo(123);
    }

    @Test
    public void configValue_withValue() {
        ConfigValue configValue = buildConfigValue();

        ConfigValueImpl value = (ConfigValueImpl) configValue;
        ConfigValue temp = value.withValue("overruled");
        Assertions.assertThat(temp).isInstanceOf(ConfigValueImpl.class);
        ConfigValueImpl value2 = (ConfigValueImpl) temp;

        Assertions.assertThat(value2.getName()).isEqualTo("name");
        Assertions.assertThat(value2.getValue()).isEqualTo("overruled");
        Assertions.assertThat(value2.getRawValue()).isEqualTo("rawValue");
        Assertions.assertThat(value2.getProfile()).isEqualTo("profile");
        Assertions.assertThat(value2.getSourceName()).isEqualTo("source");
        Assertions.assertThat(value2.getSourceOrdinal()).isEqualTo(123);
    }

    @Test
    public void configValue_withName() {
        ConfigValue configValue = buildConfigValue();

        ConfigValueImpl value = (ConfigValueImpl) configValue;
        ConfigValue temp = value.withName("overruled");
        Assertions.assertThat(temp).isInstanceOf(ConfigValueImpl.class);
        ConfigValueImpl value2 = (ConfigValueImpl) temp;

        Assertions.assertThat(value2.getName()).isEqualTo("overruled");
        Assertions.assertThat(value2.getValue()).isEqualTo("value");
        Assertions.assertThat(value2.getRawValue()).isEqualTo("rawValue");
        Assertions.assertThat(value2.getProfile()).isEqualTo("profile");
        Assertions.assertThat(value2.getSourceName()).isEqualTo("source");
        Assertions.assertThat(value2.getSourceOrdinal()).isEqualTo(123);
    }

    @Test
    public void configValue_withProfile() {
        ConfigValue configValue = buildConfigValue();

        ConfigValueImpl value = (ConfigValueImpl) configValue;
        ConfigValue temp = value.withProfile("overruled");
        Assertions.assertThat(temp).isInstanceOf(ConfigValueImpl.class);
        ConfigValueImpl value2 = (ConfigValueImpl) temp;

        Assertions.assertThat(value2.getName()).isEqualTo("name");
        Assertions.assertThat(value2.getValue()).isEqualTo("value");
        Assertions.assertThat(value2.getRawValue()).isEqualTo("rawValue");
        Assertions.assertThat(value2.getProfile()).isEqualTo("overruled");
        Assertions.assertThat(value2.getSourceName()).isEqualTo("source");
        Assertions.assertThat(value2.getSourceOrdinal()).isEqualTo(123);
    }

    private ConfigValue buildConfigValue() {
        ConfigValue configValue = ConfigValueImpl.builder()
                .withName("name")
                .withValue("value")
                .withRawValue("rawValue")
                .withProfile("profile")
                .withConfigSourceName("source")
                .withConfigSourceOrdinal(123)
                .build();

        Assertions.assertThat(configValue).isInstanceOf(ConfigValueImpl.class);
        return configValue;
    }

    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(ConfigValueImpl.class)
                .withOnlyTheseFields("name", "configSourceName", "value", "rawValue")
                .withNonnullFields("name")
                .verify();
    }

}