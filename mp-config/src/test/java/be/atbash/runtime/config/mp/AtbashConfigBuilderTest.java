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

import be.atbash.runtime.config.mp.converter.testclass.CustomIntegerConverter;
import be.atbash.runtime.config.mp.converter.testclass.SomeClassConverter1;
import be.atbash.runtime.config.mp.converter.testclass.SomeClassConverter2;
import be.atbash.runtime.config.mp.util.testclass.SomeClass;
import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Optional;

class AtbashConfigBuilderTest {

    @Test
    public void build_minimal() {
        AtbashConfigProviderResolver resolver = new AtbashConfigProviderResolver();
        Config config = resolver.getBuilder().build();

        Assertions.assertThat(config.getConfigSources()).isEmpty();  // By default, no configsources Added
        Assertions.assertThat(config.getConverter(URI.class)).isNotNull();  // By default, all converters added (this is just an example)
        Assertions.assertThat(config.getConverter(SomeClass.class)).isNotPresent(); // But not the discovered converters

    }

    @Test
    public void build_withConverter() {
        AtbashConfigProviderResolver resolver = new AtbashConfigProviderResolver();
        Config config = resolver.getBuilder().addDiscoveredConverters().build();

        Assertions.assertThat(config.getConverter(SomeClass.class)).isPresent();

    }

    @Test
    public void build_converterWithHighestPriorityIsUsed() {
        AtbashConfigProviderResolver resolver = new AtbashConfigProviderResolver();
        Config config = resolver.getBuilder()
                .addDiscoveredConverters()
                .withConverter(SomeClass.class, 1000, new SomeClassConverter2())
                .build();

        Optional<Converter<SomeClass>> converter = config.getConverter(SomeClass.class);
        Assertions.assertThat(converter).isPresent();

        Assertions.assertThat(converter.get().convert("XX").getFieldName()).isEqualTo("XX-Converter2");

    }

    @Test
    public void build_withConvertersDiscoveringTheType() {
        AtbashConfigProviderResolver resolver = new AtbashConfigProviderResolver();
        Config config = resolver.getBuilder()
                .withConverters(new SomeClassConverter1())
                .build();

        Optional<Converter<SomeClass>> converter = config.getConverter(SomeClass.class);
        Assertions.assertThat(converter).isPresent();
        Assertions.assertThat(converter.get().convert("XX").getFieldName()).isEqualTo("XX-Converter1");
    }

    @Test
    public void build_overwriteBuiltinConverter() {
        AtbashConfigProviderResolver resolver = new AtbashConfigProviderResolver();
        Config config = resolver.getBuilder()
                .withConverter(Integer.class, 1000, new CustomIntegerConverter())
                .build();

        Optional<Converter<Integer>> converter = config.getConverter(Integer.class);
        Assertions.assertThat(converter).isPresent();

        Assertions.assertThat(converter.get().convert("XX")).isEqualTo(76543);
    }

}