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

import be.atbash.ee.security.octopus.nimbus.jwt.proc.DefaultJWTProcessor;
import be.atbash.runtime.security.jwt.JWTAuthContextInfoProvider;
import be.atbash.runtime.security.jwt.principal.JWTAuthContextInfo;
import jakarta.enterprise.inject.spi.CDI;

public class RuntimeJWTProcessor extends DefaultJWTProcessor {


    public RuntimeJWTProcessor() {
        JWTAuthContextInfo authContextInfo =
                CDI.current()
                        .select(JWTAuthContextInfoProvider.class)
                        .get()
                        .getContextInfo();

        setJWSVerifierFactory(new RuntimeJWSVerifierFactory(authContextInfo));
    }

}
