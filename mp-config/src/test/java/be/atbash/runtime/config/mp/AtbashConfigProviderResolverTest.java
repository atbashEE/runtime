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
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;

import java.util.Set;

class AtbashConfigProviderResolverTest {

    @Test
    void getConfig() {
        AtbashConfigProviderResolver resolver = new AtbashConfigProviderResolver();
        Config config = resolver.getConfig();
        Assertions.assertThat(config).isNotNull();
    }

    @Test
    void getConfig_cached() {
        AtbashConfigProviderResolver resolver = new AtbashConfigProviderResolver();
        Config config1 = resolver.getConfig();
        Assertions.assertThat(config1).isNotNull();
        Config config2 = resolver.getConfig();
        Assertions.assertThat(config1 == config2).isTrue();
    }

    @Test
    void registerConfig() {

        AtbashConfigProviderResolver resolver = new AtbashConfigProviderResolver();
        Config config = resolver.getConfig();

        IllegalStateException thrownException = Assertions.catchThrowableOfType(
                () -> resolver.registerConfig(config, null),
                IllegalStateException.class);

        Assertions.assertThat(thrownException.getMessage()).isEqualTo("MPCONFIG-017: Configuration already registered for the given class loader");
    }

    @Test
    void registerConfig_withNullConfig() {

        AtbashConfigProviderResolver resolver = new AtbashConfigProviderResolver();

        IllegalArgumentException thrownException = Assertions.catchThrowableOfType(
                () -> resolver.registerConfig(null, null),
                IllegalArgumentException.class);

        Assertions.assertThat(thrownException.getMessage()).isEqualTo("MPCONFIG-016: Config cannot be null");
    }


    @Test
    void releaseConfig() {
        AtbashConfigProviderResolver resolver = new AtbashConfigProviderResolver();
        AutoCloseableConfigSource configSource = new AutoCloseableConfigSource(false);
        AutoCloseableConverter converter = new AutoCloseableConverter(false);
        Config config = resolver.getBuilder()
                .withConverter(String.class, 5000, converter)
                .withSources(configSource)
                .build();

        resolver.releaseConfig(config);

        Assertions.assertThat(configSource.isCloseCalled).isTrue();
        Assertions.assertThat(converter.isCloseCalled).isTrue();
    }

    @Test
    void releaseConfig_exceptionOnClose() {
        AtbashConfigProviderResolver resolver = new AtbashConfigProviderResolver();
        AutoCloseableConfigSource configSource = new AutoCloseableConfigSource(true);
        AutoCloseableConverter converter = new AutoCloseableConverter(true);
        Config config = resolver.getBuilder()
                .withConverter(String.class, 5000, converter)
                .withSources(configSource)
                .build();

        resolver.releaseConfig(config);

        Assertions.assertThat(configSource.isCloseCalled).isTrue();
        Assertions.assertThat(converter.isCloseCalled).isTrue();
    }

    private static class AutoCloseableConfigSource implements ConfigSource, AutoCloseable {

        private boolean isCloseCalled;
        private final boolean exceptionOnClose;

        private AutoCloseableConfigSource(boolean exceptionOnClose) {
            this.exceptionOnClose = exceptionOnClose;
        }

        @Override
        public void close() throws Exception {
            isCloseCalled = true;
            if (exceptionOnClose) {
                throw new IllegalStateException("Exception on closure of ConfigSource");
            }
        }


        @Override
        public Set<String> getPropertyNames() {
            return null;
        }

        @Override
        public String getValue(String propertyName) {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }
    }

    private static class AutoCloseableConverter implements Converter<String>, AutoCloseable {

        private boolean isCloseCalled;
        private final boolean exceptionOnClose;

        private AutoCloseableConverter(boolean exceptionOnClose) {
            this.exceptionOnClose = exceptionOnClose;
        }

        @Override
        public void close() throws Exception {
            isCloseCalled = true;
            if (exceptionOnClose) {
                throw new IllegalStateException("Exception on closure of Conerter");
            }

        }

        @Override
        public String convert(String value) throws IllegalArgumentException, NullPointerException {
            return null;
        }
    }
}