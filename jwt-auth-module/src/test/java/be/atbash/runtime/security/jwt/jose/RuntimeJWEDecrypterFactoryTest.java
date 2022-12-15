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
package be.atbash.runtime.security.jwt.jose;

import be.atbash.ee.security.octopus.keys.selector.AsymmetricPart;
import be.atbash.ee.security.octopus.keys.selector.SelectorCriteria;
import be.atbash.ee.security.octopus.nimbus.jose.JOSEException;
import be.atbash.ee.security.octopus.nimbus.jwt.jwe.EncryptionMethod;
import be.atbash.ee.security.octopus.nimbus.jwt.jwe.JWEAlgorithm;
import be.atbash.ee.security.octopus.nimbus.jwt.jwe.JWEDecrypter;
import be.atbash.ee.security.octopus.nimbus.jwt.jwe.JWEHeader;
import be.atbash.runtime.security.jwt.principal.JWTAuthContextInfo;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.util.ArrayList;
import java.util.List;

class RuntimeJWEDecrypterFactoryTest {

    @Test
    void createJWEDecrypter_noRestriction() {
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setEncryptionAlgorithms(new ArrayList<>());
        RuntimeJWEDecrypterFactory factory = new RuntimeJWEDecrypterFactory(authContextInfo);

        JWEHeader header = new JWEHeader(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A128GCM);

        SelectorCriteria criteria = SelectorCriteria.newBuilder()
                .withAsymmetricPart(AsymmetricPart.PRIVATE)
                .build();
        Key key = TestKeys.generateRSAKeys("kid", criteria).get(0).getKey();

        JWEDecrypter decrypter = factory.createJWEDecrypter(header, key);
        Assertions.assertThat(decrypter).isNotNull();
    }

    @Test
    void createJWEDecrypter_SingleRestriction() {
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setEncryptionAlgorithms(List.of(JWEAlgorithm.RSA_OAEP_384));
        RuntimeJWEDecrypterFactory factory = new RuntimeJWEDecrypterFactory(authContextInfo);

        JWEHeader header = new JWEHeader(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A128GCM);

        SelectorCriteria criteria = SelectorCriteria.newBuilder()
                .withAsymmetricPart(AsymmetricPart.PRIVATE)
                .build();
        Key key = TestKeys.generateRSAKeys("kid", criteria).get(0).getKey();

        Assertions.assertThatThrownBy(() ->
                        factory.createJWEDecrypter(header, key))
                .isInstanceOf(JOSEException.class)
                .hasMessage("Unsupported JWE algorithm: RSA-OAEP-256");
    }

    @Test
    void createJWEDecrypter_MultipleRestriction() {
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setEncryptionAlgorithms(List.of(JWEAlgorithm.RSA_OAEP_256, JWEAlgorithm.RSA_OAEP_384));
        RuntimeJWEDecrypterFactory factory = new RuntimeJWEDecrypterFactory(authContextInfo);

        JWEHeader header = new JWEHeader(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A128GCM);

        SelectorCriteria criteria = SelectorCriteria.newBuilder()
                .withAsymmetricPart(AsymmetricPart.PRIVATE)
                .build();
        Key key = TestKeys.generateRSAKeys("kid", criteria).get(0).getKey();

        JWEDecrypter decrypter = factory.createJWEDecrypter(header, key);
        Assertions.assertThat(decrypter).isNotNull();

    }
}