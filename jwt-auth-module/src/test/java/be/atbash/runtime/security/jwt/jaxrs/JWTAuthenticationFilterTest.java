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
package be.atbash.runtime.security.jwt.jaxrs;

import be.atbash.ee.security.octopus.jwt.InvalidJWTException;
import be.atbash.ee.security.octopus.nimbus.jose.JOSEException;
import be.atbash.ee.security.octopus.nimbus.jwt.JWTClaimsSet;
import be.atbash.runtime.security.jwt.JWTAuthContextInfoProvider;
import be.atbash.runtime.security.jwt.inject.PrincipalProducer;
import be.atbash.runtime.security.jwt.principal.DefaultJWTCallerPrincipal;
import be.atbash.runtime.security.jwt.principal.JWTAuthContextInfo;
import be.atbash.runtime.security.jwt.principal.JWTCallerPrincipal;
import be.atbash.runtime.security.jwt.principal.JWTCallerPrincipalFactory;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

@ExtendWith(MockitoExtension.class)
class JWTAuthenticationFilterTest {

    @Mock
    private JWTAuthContextInfoProvider authContextInfoProviderMock;

    @Mock
    private JWTCallerPrincipalFactory jwtParserMock;

    @Mock
    private PrincipalProducer producerMock;

    @Mock
    private ContainerRequestContext requestContextMock;

    @InjectMocks
    private JWTAuthenticationFilter filter;

    @Test
    void filter() throws IOException {
        // Setup retrieval of bearerToken
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setTokenHeader("Authorization");
        Mockito.when(authContextInfoProviderMock.getContextInfo()).thenReturn(authContextInfo);

        Mockito.when(requestContextMock.getHeaderString("Authorization")).thenReturn("Bearer theJWTToken");

        // Define JsonWebToken/CallerPrincipal
        JWTCallerPrincipal callerPrincipal = new DefaultJWTCallerPrincipal("theJWTToken", new JWTClaimsSet.Builder().build(), authContextInfo);
        Mockito.when(jwtParserMock.parse("theJWTToken", authContextInfo)).thenReturn(callerPrincipal);

        // Setup RequestContext
        SecurityContext securityContext = new JWTSecurityContext(null, null);
        Mockito.when(requestContextMock.getSecurityContext()).thenReturn(securityContext);
        filter.filter(requestContextMock);

        Mockito.verify(requestContextMock, Mockito.never()).abortWith(Mockito.any(Response.class));
        Mockito.verify(requestContextMock).setSecurityContext(Mockito.any(SecurityContext.class));
        Mockito.verify(producerMock).setJsonWebToken(Mockito.any(JWTCallerPrincipal.class));
    }

    @Test
    void filter_NoToken() throws IOException {
        // Setup retrieval of bearerToken
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setTokenHeader("Authorization");
        Mockito.when(authContextInfoProviderMock.getContextInfo()).thenReturn(authContextInfo);

        Mockito.when(requestContextMock.getHeaderString("Authorization")).thenReturn(null);

        // Setup RequestContext
        SecurityContext securityContext = new JWTSecurityContext(null, null);
        Mockito.when(requestContextMock.getSecurityContext()).thenReturn(securityContext);
        filter.filter(requestContextMock);

        Mockito.verify(requestContextMock, Mockito.never()).abortWith(Mockito.any(Response.class));
        Mockito.verify(requestContextMock, Mockito.never()).setSecurityContext(Mockito.any(SecurityContext.class));
        Mockito.verify(producerMock, Mockito.never()).setJsonWebToken(Mockito.any(JsonWebToken.class));
    }

    @Test
    void filter_parseInvalidJWTException() {
        // Setup retrieval of bearerToken
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setTokenHeader("Authorization");
        Mockito.when(authContextInfoProviderMock.getContextInfo()).thenReturn(authContextInfo);

        Mockito.when(requestContextMock.getHeaderString("Authorization")).thenReturn("Bearer theJWTToken");

        Mockito.when(jwtParserMock.parse("theJWTToken", authContextInfo)).thenThrow(new InvalidJWTException("Invalid JWT"));

        // Setup RequestContext
        SecurityContext securityContext = new JWTSecurityContext(null, null);
        Mockito.when(requestContextMock.getSecurityContext()).thenReturn(securityContext);
        Assertions.assertThatThrownBy(() ->
                        filter.filter(requestContextMock))
                .isInstanceOf(NotAuthorizedException.class);

        Mockito.verify(requestContextMock, Mockito.never()).abortWith(Mockito.any(Response.class));
        Mockito.verify(requestContextMock, Mockito.never()).setSecurityContext(Mockito.any(SecurityContext.class));
        Mockito.verify(producerMock, Mockito.never()).setJsonWebToken(Mockito.any(JWTCallerPrincipal.class));
    }

    @Test
    void filter_parseJOSEException() {
        // Setup retrieval of bearerToken
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setTokenHeader("Authorization");
        Mockito.when(authContextInfoProviderMock.getContextInfo()).thenReturn(authContextInfo);

        Mockito.when(requestContextMock.getHeaderString("Authorization")).thenReturn("Bearer theJWTToken");

        Mockito.when(jwtParserMock.parse("theJWTToken", authContextInfo)).thenThrow(new JOSEException("Invalid JWT"));

        // Setup RequestContext
        SecurityContext securityContext = new JWTSecurityContext(null, null);
        Mockito.when(requestContextMock.getSecurityContext()).thenReturn(securityContext);
        Assertions.assertThatThrownBy(() ->
                        filter.filter(requestContextMock))
                .isInstanceOf(NotAuthorizedException.class);

        Mockito.verify(requestContextMock, Mockito.never()).abortWith(Mockito.any(Response.class));
        Mockito.verify(requestContextMock, Mockito.never()).setSecurityContext(Mockito.any(SecurityContext.class));
        Mockito.verify(producerMock, Mockito.never()).setJsonWebToken(Mockito.any(JWTCallerPrincipal.class));
    }

    @Test
    void filter_parseIllegalArgumentException() {
        // Setup retrieval of bearerToken
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setTokenHeader("Authorization");
        Mockito.when(authContextInfoProviderMock.getContextInfo()).thenReturn(authContextInfo);

        Mockito.when(requestContextMock.getHeaderString("Authorization")).thenReturn("Bearer theJWTToken");

        Mockito.when(jwtParserMock.parse("theJWTToken", authContextInfo)).thenThrow(new IllegalArgumentException("Unknown JWT Type"));

        // Setup RequestContext
        SecurityContext securityContext = new JWTSecurityContext(null, null);
        Mockito.when(requestContextMock.getSecurityContext()).thenReturn(securityContext);
        Assertions.assertThatThrownBy(() ->
                        filter.filter(requestContextMock))
                .isInstanceOf(NotAuthorizedException.class);

        Mockito.verify(requestContextMock, Mockito.never()).abortWith(Mockito.any(Response.class));
        Mockito.verify(requestContextMock, Mockito.never()).setSecurityContext(Mockito.any(SecurityContext.class));
        Mockito.verify(producerMock, Mockito.never()).setJsonWebToken(Mockito.any(JWTCallerPrincipal.class));
    }

    @Test
    void filter_parseException() {
        // Setup retrieval of bearerToken
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setTokenHeader("Authorization");
        Mockito.when(authContextInfoProviderMock.getContextInfo()).thenReturn(authContextInfo);

        Mockito.when(requestContextMock.getHeaderString("Authorization")).thenReturn("Bearer theJWTToken");

        Mockito.when(jwtParserMock.parse("theJWTToken", authContextInfo)).thenThrow(new NullPointerException());

        // Setup RequestContext
        SecurityContext securityContext = new JWTSecurityContext(null, null);
        Mockito.when(requestContextMock.getSecurityContext()).thenReturn(securityContext);
        Assertions.assertThatThrownBy(() ->
                        filter.filter(requestContextMock))
                .isInstanceOf(InternalServerErrorException.class);

        Mockito.verify(requestContextMock, Mockito.never()).abortWith(Mockito.any(Response.class));
        Mockito.verify(requestContextMock, Mockito.never()).setSecurityContext(Mockito.any(SecurityContext.class));
        Mockito.verify(producerMock, Mockito.never()).setJsonWebToken(Mockito.any(JWTCallerPrincipal.class));
    }

    @Test
    void filter_alreadyAPrincipal() throws IOException {
        JsonWebToken webToken = new DefaultJWTCallerPrincipal(null, null, null);
        SecurityContext securityContext = new JWTSecurityContext(null, webToken);
        // We set a SecurityContext with a JsonWebToken as principal -> nothing to do.
        Mockito.when(requestContextMock.getSecurityContext()).thenReturn(securityContext);
        filter.filter(requestContextMock);

        // Not aborted but also not setting a new SecurityContext.
        Mockito.verify(requestContextMock, Mockito.never()).abortWith(Mockito.any(Response.class));
        Mockito.verify(requestContextMock, Mockito.never()).setSecurityContext(Mockito.any(SecurityContext.class));
        Mockito.verify(producerMock, Mockito.never()).setJsonWebToken(Mockito.any(JsonWebToken.class));
    }
}