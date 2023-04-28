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
package be.atbash.runtime.security.jwt.jaxrs;

import be.atbash.ee.security.octopus.jwt.InvalidJWTException;
import be.atbash.ee.security.octopus.nimbus.jose.JOSEException;
import be.atbash.ee.security.octopus.nimbus.jwt.JWTClaimsSet;
import be.atbash.runtime.logging.testing.TestLogMessages;
import be.atbash.runtime.security.jwt.JWTAuthContextInfoProvider;
import be.atbash.runtime.security.jwt.inject.PrincipalProducer;
import be.atbash.runtime.security.jwt.module.LogTracingHelper;
import be.atbash.runtime.security.jwt.principal.DefaultJWTCallerPrincipal;
import be.atbash.runtime.security.jwt.principal.JWTAuthContextInfo;
import be.atbash.runtime.security.jwt.principal.JWTCallerPrincipal;
import be.atbash.runtime.security.jwt.principal.JWTCallerPrincipalFactory;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.util.List;

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

    @Mock
    private UriInfo uriInfoMock;

    @InjectMocks
    private JWTAuthenticationFilter filter;

    @AfterEach
    public void cleanup() {
        TestLogMessages.reset();
    }

    @Test
    void filter() throws IOException {
        TestLogMessages.init();
        // Setup retrieval of bearerToken
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setTokenHeader("Authorization");
        Mockito.when(authContextInfoProviderMock.getContextInfo()).thenReturn(authContextInfo);

        Mockito.when(requestContextMock.getHeaderString("Authorization")).thenReturn("Bearer theJWTToken");
        configureBaseUri("/root");

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

        Assertions.assertThat(TestLogMessages.getLoggingEvents()).isEmpty();
    }

    @Test
    void filter_withTracing() throws IOException {
        TestLogMessages.init();

        LogTracingHelper.getInstance().storeLogTracingActive("/root", true);

        // Setup retrieval of bearerToken
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setTokenHeader("Authorization");
        authContextInfo.setGroupsClaimName("groups");
        Mockito.when(authContextInfoProviderMock.getContextInfo()).thenReturn(authContextInfo);

        Mockito.when(requestContextMock.getHeaderString("Authorization")).thenReturn("Bearer theJWTToken");
        configureUriInfo("/root");

        // Define JsonWebToken/CallerPrincipal
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("JUnit")
                .claim("groups", List.of("role1", "role2"))
                .build();
        JWTCallerPrincipal callerPrincipal = new DefaultJWTCallerPrincipal("theJWTToken", claimsSet, authContextInfo);
        Mockito.when(jwtParserMock.parse("theJWTToken", authContextInfo)).thenReturn(callerPrincipal);

        // Setup RequestContext
        SecurityContext securityContext = new JWTSecurityContext(null, null);
        Mockito.when(requestContextMock.getSecurityContext()).thenReturn(securityContext);
        filter.filter(requestContextMock);

        Mockito.verify(requestContextMock, Mockito.never()).abortWith(Mockito.any(Response.class));
        Mockito.verify(requestContextMock).setSecurityContext(Mockito.any(SecurityContext.class));
        Mockito.verify(producerMock).setJsonWebToken(Mockito.any(JWTCallerPrincipal.class));

        Assertions.assertThat(TestLogMessages.getLoggingEvents()).hasSize(3);

        Assertions.assertThat(TestLogMessages.getLoggingEvents().get(0).getMessage()).startsWith("JWT-050");
        Assertions.assertThat(TestLogMessages.getLoggingEvents().get(0).getArguments().get(0)).isEqualTo("Received request on http://localhost:8080/root/endpoint");
        Assertions.assertThat(TestLogMessages.getLoggingEvents().get(0).getMdc().get(LogTracingHelper.MDC_KEY_REQUEST_ID)).isNotBlank();

        Assertions.assertThat(TestLogMessages.getLoggingEvents().get(1).getMessage()).startsWith("JWT-050");
        Assertions.assertThat(TestLogMessages.getLoggingEvents().get(1).getArguments().get(0)).isEqualTo("Bearer token 'theJWTToken'");
        Assertions.assertThat(TestLogMessages.getLoggingEvents().get(1).getMdc().get(LogTracingHelper.MDC_KEY_REQUEST_ID)).isNotBlank();

        Assertions.assertThat(TestLogMessages.getLoggingEvents().get(2).getMessage()).startsWith("JWT-050");
        Assertions.assertThat(TestLogMessages.getLoggingEvents().get(2).getArguments().get(0)).isEqualTo("The Token was accepted and has name = 'JUnit' and roles = '[role1, role2]'");
        Assertions.assertThat(TestLogMessages.getLoggingEvents().get(2).getMdc().get(LogTracingHelper.MDC_KEY_REQUEST_ID)).isNotBlank();
    }

    private void configureUriInfo(String contextRoot) {

        URI baseUri = URI.create("http://localhost:8080" + contextRoot);
        URI requestUri = URI.create("http://localhost:8080" + contextRoot+"/endpoint");
        Mockito.when(uriInfoMock.getBaseUri()).thenReturn(baseUri);
        Mockito.when(uriInfoMock.getRequestUri()).thenReturn(requestUri);
        Mockito.when(requestContextMock.getUriInfo()).thenReturn(uriInfoMock);
    }

    private void configureBaseUri(String contextRoot) {

        URI baseUri = URI.create("http://localhost:8080" + contextRoot);
        Mockito.when(uriInfoMock.getBaseUri()).thenReturn(baseUri);
        Mockito.when(requestContextMock.getUriInfo()).thenReturn(uriInfoMock);
    }

    @Test
    void filter_NoToken() throws IOException {
        TestLogMessages.init();

        LogTracingHelper.getInstance().storeLogTracingActive("/root", true);

        // Setup retrieval of bearerToken
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setTokenHeader("Authorization");
        Mockito.when(authContextInfoProviderMock.getContextInfo()).thenReturn(authContextInfo);

        Mockito.when(requestContextMock.getHeaderString("Authorization")).thenReturn(null);

        configureUriInfo("/root");

        // Setup RequestContext
        SecurityContext securityContext = new JWTSecurityContext(null, null);
        Mockito.when(requestContextMock.getSecurityContext()).thenReturn(securityContext);
        filter.filter(requestContextMock);

        Mockito.verify(requestContextMock, Mockito.never()).abortWith(Mockito.any(Response.class));
        Mockito.verify(requestContextMock, Mockito.never()).setSecurityContext(Mockito.any(SecurityContext.class));
        Mockito.verify(producerMock, Mockito.never()).setJsonWebToken(Mockito.any(JsonWebToken.class));

        Assertions.assertThat(TestLogMessages.getLoggingEvents()).hasSize(2);

        Assertions.assertThat(TestLogMessages.getLoggingEvents().get(0).getMessage()).startsWith("JWT-050");
        Assertions.assertThat(TestLogMessages.getLoggingEvents().get(0).getArguments().get(0)).isEqualTo("Received request on http://localhost:8080/root/endpoint");
        Assertions.assertThat(TestLogMessages.getLoggingEvents().get(0).getMdc().get(LogTracingHelper.MDC_KEY_REQUEST_ID)).isNotBlank();

        Assertions.assertThat(TestLogMessages.getLoggingEvents().get(1).getMessage()).startsWith("JWT-050");
        Assertions.assertThat(TestLogMessages.getLoggingEvents().get(1).getArguments().get(0)).isEqualTo("Bearer token 'null'");
        Assertions.assertThat(TestLogMessages.getLoggingEvents().get(1).getMdc().get(LogTracingHelper.MDC_KEY_REQUEST_ID)).isNotBlank();

    }

    @Test
    void filter_parseInvalidJWTException() {
        // Setup retrieval of bearerToken
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setTokenHeader("Authorization");
        Mockito.when(authContextInfoProviderMock.getContextInfo()).thenReturn(authContextInfo);

        Mockito.when(requestContextMock.getHeaderString("Authorization")).thenReturn("Bearer theJWTToken");

        Mockito.when(jwtParserMock.parse("theJWTToken", authContextInfo)).thenThrow(new InvalidJWTException("Invalid JWT"));

        configureBaseUri("/root");

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

        configureUriInfo("/root");

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

        configureBaseUri("/root");
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

        configureBaseUri("/root");

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

        configureBaseUri("/root");

        filter.filter(requestContextMock);

        // Not aborted but also not setting a new SecurityContext.
        Mockito.verify(requestContextMock, Mockito.never()).abortWith(Mockito.any(Response.class));
        Mockito.verify(requestContextMock, Mockito.never()).setSecurityContext(Mockito.any(SecurityContext.class));
        Mockito.verify(producerMock, Mockito.never()).setJsonWebToken(Mockito.any(JsonWebToken.class));
    }
}