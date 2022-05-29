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
package be.atbash.runtime.security.jwt.inject;

import jakarta.enterprise.inject.spi.InjectionPoint;
import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.jwt.ClaimLiteral;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class CommonJwtProducerTest {

    @Mock
    private JsonWebToken currentTokenMock;

    @Mock
    private InjectionPoint injectionPointMock;

    @InjectMocks
    private CommonJwtProducer producer;

    @Test
    void getName_fromValue() {
        Set<Annotation> annotations = Set.of(new ClaimLiteral("custom"));
        Mockito.when(injectionPointMock.getQualifiers()).thenReturn(annotations);
        String name = producer.getName(injectionPointMock);
        Assertions.assertThat(name).isEqualTo("custom");
    }

    @Test
    void getName_fromStandard() {
        Set<Annotation> annotations = Set.of(new ClaimLiteral(Claims.exp));
        Mockito.when(injectionPointMock.getQualifiers()).thenReturn(annotations);
        String name = producer.getName(injectionPointMock);
        Assertions.assertThat(name).isEqualTo("exp");
    }

    @Test
    void getValue() {
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.of("claimValue"));
        Object value = producer.getValue("claim");
        Assertions.assertThat(value).isEqualTo("claimValue");
    }

    @Test
    void getValue_noValue() {
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.empty());
        Object value = producer.getValue("claim");
        Assertions.assertThat(value).isNull();
    }

}