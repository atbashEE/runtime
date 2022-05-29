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
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonValue;
import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.ClaimLiteral;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.Annotation;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class RawClaimTypeProducerTest {

    @Mock
    private JsonWebToken currentTokenMock;

    @Mock
    private InjectionPoint ipMock;

    @InjectMocks
    private RawClaimTypeProducer producer;

    private long dateAsLong;

    private Date expectedDate;

    @BeforeEach
    public void setup() {
        Date now = new Date();
        dateAsLong = now.getTime() / 1000;
        expectedDate = new Date(dateAsLong * 1000);
    }

    @Test
    void getClaimAsSet_String() {
        configureInjectionPoint();
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.of("abc,def"));
        Set<String> claimAsSet = producer.getClaimAsSet(ipMock);
        Assertions.assertThat(claimAsSet).containsExactly("abc", "def");
    }

    @Test
    void getClaimAsSet_List() {
        configureInjectionPoint();
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.of(List.of("abc", "def", "abc")));
        Set<String> claimAsSet = producer.getClaimAsSet(ipMock);
        Assertions.assertThat(claimAsSet).containsExactly("abc", "def");
    }

    @Test
    void getClaimAsSet_JsonArray() {
        configureInjectionPoint();
        JsonArray arr = Json.createArrayBuilder()
                .add(123)
                .add(456)
                .build();
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.of(arr));
        Set<String> claimAsSet = producer.getClaimAsSet(ipMock);
        Assertions.assertThat(claimAsSet).containsExactly("123", "456");
    }

    @Test
    void getClaimAsSet_null() {
        configureInjectionPoint();
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.empty());
        Set<String> claimAsSet = producer.getClaimAsSet(ipMock);
        Assertions.assertThat(claimAsSet).isNull();
    }

    @Test
    void getClaimAsString() {
        configureInjectionPoint();
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.of("ClaimValue"));
        String claimAsString = producer.getClaimAsString(ipMock);
        Assertions.assertThat(claimAsString).isEqualTo("ClaimValue");

    }

    @Test
    void getClaimAsString_json() {
        configureInjectionPoint();
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.of(Json.createValue("JsonClaimValue")));
        String claimAsString = producer.getClaimAsString(ipMock);
        Assertions.assertThat(claimAsString).isEqualTo("JsonClaimValue");

    }

    @Test
    void getClaimAsString_pojo() {
        configureInjectionPoint();
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.of(new Pojo("PojoToStringValue")));
        String claimAsString = producer.getClaimAsString(ipMock);
        Assertions.assertThat(claimAsString).isEqualTo("PojoToStringValue");

    }

    @Test
    void getClaimAsString_null() {
        configureInjectionPoint();
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.empty());
        String claimAsString = producer.getClaimAsString(ipMock);
        Assertions.assertThat(claimAsString).isNull();

    }

    @Test
    void getClaimAsLong() {
        configureInjectionPoint();
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.of(123L));
        Long claimAsLong = producer.getClaimAsLong(ipMock);
        Assertions.assertThat(claimAsLong).isEqualTo(123L);

    }

    @Test
    void getClaimAsLong_json() {
        configureInjectionPoint();
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.of(Json.createValue(123L)));
        Long claimAsLong = producer.getClaimAsLong(ipMock);
        Assertions.assertThat(claimAsLong).isEqualTo(123L);

    }

    @Test
    void getClaimAsLong_pojo() {
        configureInjectionPoint();
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.of(new Pojo("123")));
        Long claimAsLong = producer.getClaimAsLong(ipMock);
        Assertions.assertThat(claimAsLong).isEqualTo(123L);

    }

    @Test
    void getClaimAsLong_null() {
        configureInjectionPoint();
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.empty());
        Long claimAsLong = producer.getClaimAsLong(ipMock);
        Assertions.assertThat(claimAsLong).isNull();

    }

    private void configureInjectionPoint() {
        Set<Annotation> qualifiers = new HashSet<>();
        qualifiers.add(new ClaimLiteral("claim"));
        Mockito.when(ipMock.getQualifiers()).thenReturn(qualifiers);
    }


    @Test
    void getClaimAsDate() {
        configureInjectionPoint();
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.of(dateAsLong));
        Date claimAsDate = producer.getClaimAsDate(ipMock);
        Assertions.assertThat(claimAsDate).isEqualTo(expectedDate);

    }

    @Test
    void getClaimAsDate_json() {
        configureInjectionPoint();
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.of(Json.createValue(dateAsLong)));
        Date claimAsDate = producer.getClaimAsDate(ipMock);
        Assertions.assertThat(claimAsDate).isEqualTo(expectedDate);

    }

    @Test
    void getClaimAsDate_pojo() {
        configureInjectionPoint();
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.of(new Pojo(String.valueOf(dateAsLong))));
        Date claimAsDate = producer.getClaimAsDate(ipMock);
        Assertions.assertThat(claimAsDate).isEqualTo(expectedDate);

    }

    @Test
    void getClaimAsDate_null() {
        configureInjectionPoint();
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.empty());
        Date claimAsDate = producer.getClaimAsDate(ipMock);
        Assertions.assertThat(claimAsDate).isNull();

    }

    @Test
    void getClaimAsDouble() {
        configureInjectionPoint();
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.of(543.21D));
        Double claimAsDouble = producer.getClaimAsDouble(ipMock);
        Assertions.assertThat(claimAsDouble).isEqualTo(543.21D);

    }

    @Test
    void getClaimAsDouble_json() {
        configureInjectionPoint();
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.of(Json.createValue(543.21D)));
        Double claimAsDouble = producer.getClaimAsDouble(ipMock);
        Assertions.assertThat(claimAsDouble).isEqualTo(543.21D);

    }

    @Test
    void getClaimAsDouble_pojo() {
        configureInjectionPoint();
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.of(new Pojo("543.21")));
        Double claimAsDouble = producer.getClaimAsDouble(ipMock);
        Assertions.assertThat(claimAsDouble).isEqualTo(543.21D);

    }

    @Test
    void getClaimAsDouble_null() {
        configureInjectionPoint();
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.empty());
        Double claimAsDouble = producer.getClaimAsDouble(ipMock);
        Assertions.assertThat(claimAsDouble).isNull();

    }

    @Test
    void getClaimAsBoolean() {
        configureInjectionPoint();
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.of(Boolean.TRUE));
        Boolean claimAsBoolean = producer.getClaimAsBoolean(ipMock);
        Assertions.assertThat(claimAsBoolean).isEqualTo(Boolean.TRUE);

    }

    @Test
    void getClaimAsBoolean_json() {
        configureInjectionPoint();
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.of(JsonValue.TRUE));
        Boolean claimAsBoolean = producer.getClaimAsBoolean(ipMock);
        Assertions.assertThat(claimAsBoolean).isEqualTo(Boolean.TRUE);

    }

    @Test
    void getClaimAsBoolean_String() {
        configureInjectionPoint();
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.of("true"));
        Boolean claimAsBoolean = producer.getClaimAsBoolean(ipMock);
        Assertions.assertThat(claimAsBoolean).isEqualTo(Boolean.TRUE);

    }

    @Test
    void getClaimAsBoolean_null() {
        configureInjectionPoint();
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.empty());
        Boolean claimAsBoolean = producer.getClaimAsBoolean(ipMock);
        Assertions.assertThat(claimAsBoolean).isNull();

    }

    @Test
    void getOptionalValue_String() {
        configureInjectionPoint();
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.of("ClaimValue"));
        Optional<String> optionalString = producer.getOptionalValue(ipMock);
        Assertions.assertThat(optionalString).isEqualTo(Optional.of("ClaimValue"));
    }

    @Test
    void getOptionalValue_Long() {
        configureInjectionPoint();
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.of(123L));
        Optional<Long> optionalLong = producer.getOptionalValue(ipMock);
        Assertions.assertThat(optionalLong).isEqualTo(Optional.of(123L));

    }

    @Test
    void getOptionalValue_StringToLong() {
        configureInjectionPoint();
        Mockito.when(currentTokenMock.claim("claim")).thenReturn(Optional.of("ClaimValue"));
        Optional<Long> optionalLong = producer.getOptionalValue(ipMock);
        // How you can trick the Java type system :)
        Assertions.assertThat(optionalLong).isEqualTo(Optional.of("ClaimValue"));
    }

    private static class Pojo {
        // TODO Does it make sense to test with a POJO since we will only have JsonObject and
        // not an actual Java Class where we can customize the Java instance from the JWT token.
        private final String stringValue;

        private Pojo(String toStringValue) {
            stringValue = toStringValue;
        }

        @Override
        public String toString() {
            return stringValue;
        }
    }
}