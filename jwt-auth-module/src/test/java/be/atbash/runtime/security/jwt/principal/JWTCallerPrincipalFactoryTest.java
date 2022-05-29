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
package be.atbash.runtime.security.jwt.principal;

import be.atbash.ee.security.octopus.jwt.InvalidJWTException;
import be.atbash.ee.security.octopus.jwt.JWTEncoding;
import be.atbash.ee.security.octopus.jwt.decoder.JWTData;
import be.atbash.ee.security.octopus.jwt.decoder.JWTDecoder;
import be.atbash.ee.security.octopus.keys.selector.KeySelector;
import be.atbash.ee.security.octopus.nimbus.jwt.JWTClaimsSet;
import be.atbash.runtime.core.data.util.SystemPropertyUtil;
import be.atbash.util.TestReflectionUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class JWTCallerPrincipalFactoryTest {

    @Mock
    private JWTDecoder jwtDecoderMock;

    @Mock
    private KeySelector keySelectorMock;

    @InjectMocks
    private JWTCallerPrincipalFactory factory;

    @AfterEach
    public void cleanUp() throws NoSuchFieldException {
        System.clearProperty("atbash.runtime.tck");
        System.clearProperty("atbash.runtime.tck.jwt");
        // reset the JVM Singleton between each run !
        Map<?, ?> cache = TestReflectionUtils.getValueOf(SystemPropertyUtil.getInstance(), "resultCache");
        cache.clear();
    }

    @Test
    void parse() {
        String token = "theToken";
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .claim("claim1", "value1")
                .claim("claim2", "value2")
                .build();
        JWTData<Object> jwtData = new JWTData<>(jwtClaimsSet, null);
        Mockito.when(jwtDecoderMock.decode(Mockito.eq(token), Mockito.any(), Mockito.eq(keySelectorMock), Mockito.any(MPBearerTokenVerifier.class)))
                .thenReturn(jwtData);
        JWTCallerPrincipal principal = factory.parse(token, authContextInfo);
        Assertions.assertThat(principal).isNotNull();
        Assertions.assertThat(principal.getClaimNames()).contains("claim1", "claim2");


    }


    @Test
    void parse_jwe_tck_decryptorSpecified() {
        System.setProperty("atbash.runtime.tck.jwt", "true");

        String token = "theToken.is.a.jwe.dueToDots";
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setDecryptionKeyLocation(List.of("classpath:private.key"));

        Mockito.when(jwtDecoderMock.determineEncoding(token)).thenReturn(JWTEncoding.JWE);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .claim("claim1", "value1")
                .claim("claim2", "value2")
                .build();
        JWTData<Object> jwtData = new JWTData<>(jwtClaimsSet, null);
        Mockito.when(jwtDecoderMock.decode(Mockito.eq(token), Mockito.any(), Mockito.eq(keySelectorMock), Mockito.any(MPBearerTokenVerifier.class)))
                .thenReturn(jwtData);
        JWTCallerPrincipal principal = factory.parse(token, authContextInfo);
        Assertions.assertThat(principal).isNotNull();
        Assertions.assertThat(principal.getClaimNames()).contains("claim1", "claim2");


    }

    @Test
    void parse_jws_tck_decryptorSpecified() {
        System.setProperty("atbash.runtime.tck.jwt", "true");

        String token = "theToken.is.notAJWE";
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setDecryptionKeyLocation(List.of("classpath:private.key"));

        Mockito.when(jwtDecoderMock.determineEncoding(token)).thenReturn(JWTEncoding.JWS);

        Assertions.assertThatThrownBy(() -> factory.parse(token, authContextInfo)
        ).isInstanceOf(InvalidJWTException.class).hasMessage("Token must be a JWE");


    }

    @Test
    void parse_jwe_noTCK_decryptorSpecified() {
        System.setProperty("atbash.runtime.tck.jwt", "false");  // To make it explicit, is default

        String token = "theToken.is.notAJWE";
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setDecryptionKeyLocation(List.of("classpath:private.key"));

        // Lenient because is not call due to and within the test
        Mockito.lenient().when(jwtDecoderMock.determineEncoding(token)).thenReturn(JWTEncoding.JWS);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .claim("claim1", "value1")
                .claim("claim2", "value2")
                .build();
        JWTData<Object> jwtData = new JWTData<>(jwtClaimsSet, null);
        Mockito.when(jwtDecoderMock.decode(Mockito.eq(token), Mockito.any(), Mockito.eq(keySelectorMock), Mockito.any(MPBearerTokenVerifier.class)))
                .thenReturn(jwtData);
        JWTCallerPrincipal principal = factory.parse(token, authContextInfo);
        Assertions.assertThat(principal).isNotNull();
        Assertions.assertThat(principal.getClaimNames()).contains("claim1", "claim2");


    }

}