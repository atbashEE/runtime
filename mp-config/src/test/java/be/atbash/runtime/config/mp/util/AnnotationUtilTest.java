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

import be.atbash.runtime.config.mp.util.testclass.ClassWithPriority;
import be.atbash.runtime.config.mp.util.testclass.SomeClass;
import be.atbash.runtime.config.mp.util.testclass.SubClass;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.InjectionPoint;
import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.OptionalInt;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnotationUtilTest {

    @Mock
    private InjectionPoint injectionPointMock;

    @Mock
    private Annotated annotatedMock;

    @Test
    void getConfigPropertiesAnnotation() {
        when(injectionPointMock.getAnnotated()).thenReturn(annotatedMock);
        when(annotatedMock.getAnnotation(ConfigProperties.class)).thenReturn(ConfigProperties.Literal.of("Atbash"));
        ConfigProperties annotation = AnnotationUtil.getConfigPropertiesAnnotation(injectionPointMock);
        Assertions.assertThat(annotation).isNotNull();
    }

    @Test
    void getConfigPropertiesAnnotation_notPresent() {
        ConfigProperties annotation = AnnotationUtil.getConfigPropertiesAnnotation(injectionPointMock);
        Assertions.assertThat(annotation).isNull();
    }

    @Test
    void parsePrefix() {
        Optional<String> prefix = AnnotationUtil.parsePrefix(ConfigProperties.Literal.of("Atbash"));
        Assertions.assertThat(prefix).isPresent();
        Assertions.assertThat(prefix.get()).isEqualTo("Atbash.");
    }

    @Test
    void parsePrefix_noAnnotation() {
        Optional<String> prefix = AnnotationUtil.parsePrefix(null);
        Assertions.assertThat(prefix).isEmpty();

    }

    @Test
    void parsePrefix_AnnotationEmptyPrefix() {
        Optional<String> prefix = AnnotationUtil.parsePrefix(ConfigProperties.Literal.of(""));
        Assertions.assertThat(prefix).isPresent();
        Assertions.assertThat(prefix.get()).isEqualTo("");

    }

    @Test
    void parsePrefix_AnnotationUnconfiguredPrefix() {
        Optional<String> prefix = AnnotationUtil.parsePrefix(ConfigProperties.Literal.of(ConfigProperties.UNCONFIGURED_PREFIX));
        Assertions.assertThat(prefix).isEmpty();

    }

    @Test
    void getPriority() {
        OptionalInt priority = AnnotationUtil.getPriority(ClassWithPriority.class);
        Assertions.assertThat(priority).isPresent();
        Assertions.assertThat(priority.getAsInt()).isEqualTo(200);
    }

    @Test
    void getPriority_noPriority() {
        OptionalInt priority = AnnotationUtil.getPriority(SomeClass.class);
        Assertions.assertThat(priority).isEmpty();
    }


    @Test
    void getPriority_onSuperClass() {
        OptionalInt priority = AnnotationUtil.getPriority(SubClass.class);
        Assertions.assertThat(priority).isPresent();
        Assertions.assertThat(priority.getAsInt()).isEqualTo(123);
    }


}