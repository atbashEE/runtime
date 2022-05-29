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

import be.atbash.runtime.security.jwt.JWTAuthContextInfoProvider;
import be.atbash.runtime.security.jwt.exception.MissingConfigurationException;
import be.atbash.runtime.security.jwt.principal.JWTAuthContextInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ValidateJWTConfiguration {

    @Inject
    private JWTAuthContextInfoProvider jwtAuthContextInfoProvider;

    public void onApplicationDeployment(@Observes @Initialized(ApplicationScoped.class) Object o) {
        JWTAuthContextInfo contextInfo = jwtAuthContextInfoProvider.getContextInfo();

        List<String> missingKeys = new ArrayList<>();
        if (contextInfo.getIssuedBy().isEmpty()) {
            missingKeys.add("Issuer");
        }
        if (contextInfo.getPublicKeyLocation().isEmpty() && contextInfo.getPublicKeyContent() == null) {
            missingKeys.add("Key location or key content");
        }

        if (!missingKeys.isEmpty()) {
            String missing = String.join(", ", missingKeys);
            throw new MissingConfigurationException("JWT-001", missing);
        }
    }
}
