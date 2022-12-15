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
import be.atbash.ee.security.octopus.nimbus.jwt.jws.JWSAlgorithm;
import be.atbash.ee.security.octopus.nimbus.jwt.jws.JWSHeader;
import be.atbash.ee.security.octopus.nimbus.jwt.jws.JWSVerifier;
import be.atbash.runtime.security.jwt.principal.JWTAuthContextInfo;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.util.ArrayList;
import java.util.List;

class RuntimeJWSVerifierFactoryTest {

    @Test
    void createJWSVerifier_noRestriction() {
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setSignatureAlgorithms(new ArrayList<>());
        RuntimeJWSVerifierFactory factory = new RuntimeJWSVerifierFactory(authContextInfo);

        JWSHeader header = new JWSHeader(JWSAlgorithm.RS256);
        SelectorCriteria criteria = SelectorCriteria.newBuilder()
                .withAsymmetricPart(AsymmetricPart.PUBLIC)
                .build();
        Key key = TestKeys.generateRSAKeys("kid", criteria).get(0).getKey();

        JWSVerifier verifier = factory.createJWSVerifier(header, key);
        Assertions.assertThat(verifier).isNotNull();

    }

    @Test
    void createJWSVerifier_SingleRestriction() {
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setSignatureAlgorithms(List.of(JWSAlgorithm.RS384));
        RuntimeJWSVerifierFactory factory = new RuntimeJWSVerifierFactory(authContextInfo);

        JWSHeader header = new JWSHeader(JWSAlgorithm.RS256);
        SelectorCriteria criteria = SelectorCriteria.newBuilder()
                .withAsymmetricPart(AsymmetricPart.PUBLIC)
                .build();
        Key key = TestKeys.generateRSAKeys("kid", criteria).get(0).getKey();

        Assertions.assertThatThrownBy(() ->
                        factory.createJWSVerifier(header, key))
                .isInstanceOf(JOSEException.class)
                .hasMessage("Unsupported JWS algorithm: RS256");


    }

    @Test
    void createJWSVerifier_MultipleRestrictions() {
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setSignatureAlgorithms(List.of(JWSAlgorithm.RS256, JWSAlgorithm.RS384));
        RuntimeJWSVerifierFactory factory = new RuntimeJWSVerifierFactory(authContextInfo);

        JWSHeader header = new JWSHeader(JWSAlgorithm.RS256);
        SelectorCriteria criteria = SelectorCriteria.newBuilder()
                .withAsymmetricPart(AsymmetricPart.PUBLIC)
                .build();
        Key key = TestKeys.generateRSAKeys("kid", criteria).get(0).getKey();

        JWSVerifier verifier = factory.createJWSVerifier(header, key);
        Assertions.assertThat(verifier).isNotNull();

    }
}