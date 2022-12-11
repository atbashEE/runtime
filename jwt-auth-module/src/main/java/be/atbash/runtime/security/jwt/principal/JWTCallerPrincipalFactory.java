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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * An implementation that creates a JWTCallerPrincipal based on the token.
 */
@ApplicationScoped
public class JWTCallerPrincipalFactory {

    @Inject
    private JWTDecoder jwtDecoder;

    @Inject
    private KeySelector keySelector;

    public JWTCallerPrincipal parse(String token, JWTAuthContextInfo authContextInfo) {

        boolean tck = SystemPropertyUtil.getInstance().isTck("jwt");
        if (tck && authContextInfo.isJWERequired() && jwtDecoder.determineEncoding(token) != JWTEncoding.JWE) {
            throw new InvalidJWTException("Token must be a JWE");
        }

        JWTData<JWTClaimsSet> data;
        if (authContextInfo.getPublicKeyContent() != null) {
            // FIXME When inline public key defined, is JWE decryption still supported?
            data = jwtDecoder.decode(token, JWTClaimsSet.class, new InlineKeySelector(authContextInfo.getPublicKeyContent()));
        } else {
            data = jwtDecoder.decode(token, JWTClaimsSet.class, keySelector);
        }

        return new DefaultJWTCallerPrincipal(token, data.getData(), authContextInfo);

    }

}
