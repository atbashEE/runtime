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
import be.atbash.ee.security.octopus.nimbus.jose.crypto.factories.DefaultJWEDecrypterFactory;
import be.atbash.ee.security.octopus.nimbus.jwt.jwe.JWEDecrypter;
import be.atbash.ee.security.octopus.nimbus.jwt.jwe.JWEHeader;
import be.atbash.runtime.security.jwt.principal.JWTAuthContextInfo;

import java.security.Key;

public class RuntimeJWEDecrypterFactory extends DefaultJWEDecrypterFactory {

    private final JWTAuthContextInfo authContextInfo;

    public RuntimeJWEDecrypterFactory(JWTAuthContextInfo authContextInfo) {
        this.authContextInfo = authContextInfo;
    }

    @Override
    public JWEDecrypter createJWEDecrypter(JWEHeader header, Key key) {
        // Check if the algorithms that can be used are restricted.
        if (!authContextInfo.getEncryptionAlgorithms().isEmpty()) {
            if (!authContextInfo.getEncryptionAlgorithms().contains(header.getAlgorithm())) {
                throw new JOSEException("Unsupported JWE algorithm: " + header.getAlgorithm());
            }
        }
        return super.createJWEDecrypter(header, key);
    }
}
