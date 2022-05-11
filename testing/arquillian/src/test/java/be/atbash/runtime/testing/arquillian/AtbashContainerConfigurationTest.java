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
package be.atbash.runtime.testing.arquillian;

import org.assertj.core.api.Assertions;
import org.jboss.arquillian.container.spi.ConfigurationException;
import org.junit.jupiter.api.Test;

class AtbashContainerConfigurationTest {

    @Test
    void validate_noProfileSet() {
        AtbashContainerConfiguration configuration = new AtbashContainerConfiguration();
        configuration.validate();
        Assertions.assertThat(configuration.getProfile()).isEqualTo("default");
    }

    @Test
    void validate_all() {
        AtbashContainerConfiguration configuration = new AtbashContainerConfiguration();
        configuration.setProfile("all");
        configuration.validate();
        Assertions.assertThat(configuration.getProfile()).isEqualTo("all");
    }

    @Test
    void validate_caseInsensitive() {
        AtbashContainerConfiguration configuration = new AtbashContainerConfiguration();
        configuration.setProfile("DeFauLT");
        configuration.validate();
        Assertions.assertThat(configuration.getProfile()).isEqualTo("default");
    }

    @Test
    void validate_wrongProfileName() {
        AtbashContainerConfiguration configuration = new AtbashContainerConfiguration();
        configuration.setProfile("wrong");

        Assertions.assertThatThrownBy(configuration::validate)
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("Profile parameter is not valid");
    }
}