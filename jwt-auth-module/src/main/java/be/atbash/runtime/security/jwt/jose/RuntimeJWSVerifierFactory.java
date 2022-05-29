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

import be.atbash.ee.security.octopus.nimbus.jose.JOSEException;
import be.atbash.ee.security.octopus.nimbus.jose.crypto.factories.DefaultJWSVerifierFactory;
import be.atbash.ee.security.octopus.nimbus.jwt.jws.JWSHeader;
import be.atbash.ee.security.octopus.nimbus.jwt.jws.JWSVerifier;
import be.atbash.runtime.security.jwt.principal.JWTAuthContextInfo;

import java.security.Key;

public class RuntimeJWSVerifierFactory extends DefaultJWSVerifierFactory {

    private final JWTAuthContextInfo authContextInfo;

    public RuntimeJWSVerifierFactory(JWTAuthContextInfo authContextInfo) {
        this.authContextInfo = authContextInfo;
    }

    @Override
    public JWSVerifier createJWSVerifier(JWSHeader header, Key key) {
        // Check if the algorithms that can be used are restricted.
        if (!authContextInfo.getSignatureAlgorithms().isEmpty()) {
            if (!authContextInfo.getSignatureAlgorithms().contains(header.getAlgorithm())) {
                throw new JOSEException("Unsupported JWS algorithm: " + header.getAlgorithm());
            }
        }
        return super.createJWSVerifier(header, key);
    }
}
