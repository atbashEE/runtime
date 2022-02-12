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

import be.atbash.runtime.config.mp.AtbashConfig;
import be.atbash.runtime.config.mp.AtbashConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SysPropConfigSourceTest {

    @BeforeEach
    void setUp() {
        System.setProperty("config_ordinal", "1000");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("config_ordinal");
    }

    @Test
    void ordinal() {
        AtbashConfig config = new AtbashConfigBuilder().withSources(new SysPropConfigSource()).build();
        ConfigSource configSource = config.getConfigSources().iterator().next();

        assertThat(configSource).isInstanceOf(SysPropConfigSource.class);
        assertThat(configSource.getOrdinal()).isEqualTo(1000);
    }

    @Test
    void getProperties() {
        AtbashConfig config = new AtbashConfigBuilder().withSources(new SysPropConfigSource()).build();
        ConfigSource configSource = config.getConfigSources().iterator().next();

        assertThat(configSource).isInstanceOf(SysPropConfigSource.class);
        assertThat(configSource.getProperties().size()).isGreaterThan(20); // There are many system properties
        // We assume that when we have mire than 20, we have them all.

        System.out.println(configSource.getProperties());
    }

    @Test
    void getValue() {
        AtbashConfig config = new AtbashConfigBuilder().withSources(new SysPropConfigSource()).build();
        ConfigSource configSource = config.getConfigSources().iterator().next();

        assertThat(configSource).isInstanceOf(SysPropConfigSource.class);
        assertThat(configSource.getValue("java.vendor.version")).isNotNull();


    }


}