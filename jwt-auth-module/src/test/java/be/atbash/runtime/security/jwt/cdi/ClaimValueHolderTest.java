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
package be.atbash.runtime.security.jwt.cdi;

import be.atbash.runtime.security.jwt.inject.CommonJwtProducer;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.json.JsonString;
import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.jwt.ClaimValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Type;
import java.util.Optional;


@ExtendWith(MockitoExtension.class)
class ClaimValueHolderTest {

    @Mock
    private InjectionPoint injectionPointMock;

    @Mock
    private CommonJwtProducer producerMock;

    @Test
    void getName() {
        Mockito.when(producerMock.getName(injectionPointMock)).thenReturn("pointName");
        ClaimValueHolder<?> claimValueHolder = new ClaimValueHolder<>(injectionPointMock, producerMock);
        String name = claimValueHolder.getName();
        Assertions.assertThat(name).isEqualTo("pointName");
    }

    @Test
    void getValue() {
        Mockito.when(producerMock.getName(injectionPointMock)).thenReturn("claimName");
        Mockito.when(producerMock.getValue("claimName")).thenReturn("claimValue");
        Mockito.when(injectionPointMock.getType()).thenReturn(String.class);
        ClaimValueHolder<String> claimValueHolder = new ClaimValueHolder<>(injectionPointMock, producerMock);
        String value = claimValueHolder.getValue();
        Assertions.assertThat(value).isEqualTo("claimValue");
    }

    @Test
    void getValue_optional() throws NoSuchMethodException {
        Mockito.when(producerMock.getName(injectionPointMock)).thenReturn("claimName");
        Mockito.when(producerMock.getValue("claimName")).thenReturn(Optional.of("claimValue"));


        Type type = ClaimValueHolderTest.class.getDeclaredMethod("method1").getGenericReturnType();

        Mockito.when(injectionPointMock.getType()).thenReturn(type);

        ClaimValueHolder<Optional<String>> claimValueHolder = new ClaimValueHolder<>(injectionPointMock, producerMock);
        Optional<String> value = claimValueHolder.getValue();
        Assertions.assertThat(value).isPresent();
        Assertions.assertThat(value).contains("claimValue");

    }

    @Test
    void getValue_ClaimValue() throws NoSuchMethodException {
        Mockito.when(producerMock.getName(injectionPointMock)).thenReturn("claimName");
        Mockito.when(producerMock.getValue("claimName")).thenReturn(new ClaimValue<String>() {
            @Override
            public String getName() {
                return "someName";
            }

            @Override
            public String getValue() {
                return "ValueInClaim";
            }
        });


        Type type = ClaimValueHolderTest.class.getDeclaredMethod("method2").getGenericReturnType();

        Mockito.when(injectionPointMock.getType()).thenReturn(type);

        ClaimValueHolder<ClaimValue<String>> claimValueHolder = new ClaimValueHolder<>(injectionPointMock, producerMock);
        ClaimValue<String> value = claimValueHolder.getValue();
        Assertions.assertThat(value.getValue()).isEqualTo("ValueInClaim");

    }

    @Test
    void getValue_JsonValue() {
        Mockito.when(producerMock.getName(injectionPointMock)).thenReturn("claimName");
        Mockito.when(producerMock.getValue("claimName")).thenReturn("claimValue");

        Mockito.when(injectionPointMock.getType()).thenReturn(JsonString.class);

        ClaimValueHolder<JsonString> claimValueHolder = new ClaimValueHolder<>(injectionPointMock, producerMock);
        JsonString value = claimValueHolder.getValue();
        Assertions.assertThat(value.getString()).isEqualTo("claimValue");

    }

    @Test
    void getValue_OptionalClaimValue() throws NoSuchMethodException {
        Mockito.when(producerMock.getName(injectionPointMock)).thenReturn("claimName");
        Mockito.when(producerMock.getValue("claimName")).thenReturn("value");

        Type type = ClaimValueHolderTest.class.getDeclaredMethod("method3").getGenericReturnType();

        Mockito.when(injectionPointMock.getType()).thenReturn(type);

        ClaimValueHolder<Optional<String>> claimValueHolder = new ClaimValueHolder<>(injectionPointMock, producerMock);
        Optional<String> value = claimValueHolder.getValue();
        Assertions.assertThat(value).isPresent();
        Assertions.assertThat(value).contains("value");

    }

    private Optional<String> method1() {
        return null;
    }

    private ClaimValue<String> method2() {
        return null;
    }

    private ClaimValue<Optional<String>> method3() {
        return null;
    }
}