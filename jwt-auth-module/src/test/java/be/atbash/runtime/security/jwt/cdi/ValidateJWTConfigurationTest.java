/*
 * Copyright 2021-2023 Rudy De Busscher (https://www.atbash.be)
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
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ValidateJWTConfigurationTest {

    @Mock
    private JWTAuthContextInfoProvider providerMock;

    @InjectMocks
    private ValidateJWTConfiguration validateJWTConfiguration;

    @Test
    void onApplicationDeployment() {
        JWTAuthContextInfo contextInfo = new JWTAuthContextInfo();
        contextInfo.setIssuedBy(List.of("Issuer"));
        contextInfo.setPublicKeyLocation(List.of("file:key.pem"));
        Mockito.when(providerMock.getContextInfo()).thenReturn(contextInfo);
        Assertions.assertThatCode(() -> {
            validateJWTConfiguration.onApplicationDeployment(null);
        }).doesNotThrowAnyException();
    }

    @Test
    void onApplicationDeployment_withKeyContent() {
        JWTAuthContextInfo contextInfo = new JWTAuthContextInfo();
        contextInfo.setIssuedBy(List.of("Issuer"));
        contextInfo.setPublicKeyLocation(Collections.emptyList());
        contextInfo.setPublicKeyContent("SomethingThatWillBeInterpretedAsAKey");
        Mockito.when(providerMock.getContextInfo()).thenReturn(contextInfo);

        Assertions.assertThatCode(() -> {
            validateJWTConfiguration.onApplicationDeployment(null);
        }).doesNotThrowAnyException();

    }

    @Test
    void onApplicationDeployment_missingIssuer() {
        JWTAuthContextInfo contextInfo = new JWTAuthContextInfo();
        contextInfo.setIssuedBy(Collections.emptyList());
        contextInfo.setPublicKeyLocation(List.of("file:key.pem"));
        Mockito.when(providerMock.getContextInfo()).thenReturn(contextInfo);
        Assertions.assertThatThrownBy(() ->
                        validateJWTConfiguration.onApplicationDeployment(null))
                .isInstanceOf(MissingConfigurationException.class)
                .hasMessage("JWT-001");

        // We can't capture the parameters as it is already converted into the message by the constructor
        // But there is no code registered for looking up error message from ResourceBundle and thus
        //we end up with just the code as the message.
    }

    @Test
    void onApplicationDeployment_missingPublicKey() {
        JWTAuthContextInfo contextInfo = new JWTAuthContextInfo();
        contextInfo.setIssuedBy(List.of("Issuer"));
        contextInfo.setPublicKeyLocation(Collections.emptyList());
        Mockito.when(providerMock.getContextInfo()).thenReturn(contextInfo);
        Assertions.assertThatThrownBy(() ->
                        validateJWTConfiguration.onApplicationDeployment(null))
                .isInstanceOf(MissingConfigurationException.class)
                .hasMessage("JWT-001");

        // We can't capture the parameters as it is already converted into the message by the constructor
        // But there is no code registered for looking up error message from ResourceBundle and thus
        //we end up with just the code as the message.
    }
}