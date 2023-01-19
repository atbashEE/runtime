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

import be.atbash.runtime.logging.testing.TestLogMessages;
import be.atbash.runtime.security.jwt.module.LogTracingHelper;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;


@ExtendWith(MockitoExtension.class)
class RolesAllowedFilterTest {

    @Mock
    private ContainerRequestContext containerRequestContextMock;

    @Mock
    private ContainerRequestContext requestContextMock;

    @Mock
    private UriInfo uriInfoMock;

    @AfterEach
    public void cleanup() {
        TestLogMessages.reset();
    }

    @Test
    void filter() {
        TestLogMessages.init();
        activateTracing();

        RolesAllowedFilter rolesAllowedFilter = new RolesAllowedFilter(new String[]{"role"});

        Mockito.when(containerRequestContextMock.getSecurityContext()).thenReturn(new TestSecurityContext(true, "role"));
        rolesAllowedFilter.filter(containerRequestContextMock);

        Assertions.assertThat(TestLogMessages.getLoggingEvents()).hasSize(1);

        Assertions.assertThat(TestLogMessages.getLoggingEvents().get(0).getMessage()).startsWith("JWT-050");
        Assertions.assertThat(TestLogMessages.getLoggingEvents().get(0).getArguments().get(0)).isEqualTo("Request is permitted as there was a matching role (required role(s) 'role').");

    }

    @Test
    void filter_noTracing() {
        // To test if we don't have log entries by default
        TestLogMessages.init();
        noTracing();

        RolesAllowedFilter rolesAllowedFilter = new RolesAllowedFilter(new String[]{"role"});

        Mockito.when(containerRequestContextMock.getSecurityContext()).thenReturn(new TestSecurityContext(true, "role"));
        rolesAllowedFilter.filter(containerRequestContextMock);

        Assertions.assertThat(TestLogMessages.getLoggingEvents()).isEmpty();

    }

    @Test
    void filter_multiple() {
        TestLogMessages.init();
        activateTracing();

        RolesAllowedFilter rolesAllowedFilter = new RolesAllowedFilter(new String[]{"role1","role2"});

        Mockito.when(containerRequestContextMock.getSecurityContext()).thenReturn(new TestSecurityContext(true, "role2","role3"));
        rolesAllowedFilter.filter(containerRequestContextMock);

        Assertions.assertThat(TestLogMessages.getLoggingEvents()).hasSize(1);

        Assertions.assertThat(TestLogMessages.getLoggingEvents().get(0).getMessage()).startsWith("JWT-050");
        Assertions.assertThat(TestLogMessages.getLoggingEvents().get(0).getArguments().get(0)).isEqualTo("Request is permitted as there was a matching role (required role(s) 'role1,role2').");

    }

    @Test
    void filter_any() {
        TestLogMessages.init();
        activateTracing();

        RolesAllowedFilter rolesAllowedFilter = new RolesAllowedFilter(new String[]{"*"});

        Mockito.when(containerRequestContextMock.getSecurityContext()).thenReturn(new TestSecurityContext(true, "role"));
        rolesAllowedFilter.filter(containerRequestContextMock);

        Assertions.assertThat(TestLogMessages.getLoggingEvents()).hasSize(1);

        Assertions.assertThat(TestLogMessages.getLoggingEvents().get(0).getMessage()).startsWith("JWT-050");
        Assertions.assertThat(TestLogMessages.getLoggingEvents().get(0).getArguments().get(0)).isEqualTo("Request is permitted as there was a matching role (required role(s) 'Any role allowed (a role contained '*')').");

    }

    @Test
    void filter_missingRole() {
        TestLogMessages.init();
        activateTracing();

        RolesAllowedFilter rolesAllowedFilter = new RolesAllowedFilter(new String[]{"role"});

        Mockito.when(containerRequestContextMock.getSecurityContext()).thenReturn(new TestSecurityContext(true, "JUnit"));
        Assertions.assertThatThrownBy(() -> rolesAllowedFilter.filter(containerRequestContextMock))
                .isInstanceOf(ForbiddenException.class);

        Assertions.assertThat(TestLogMessages.getLoggingEvents()).hasSize(1);

        Assertions.assertThat(TestLogMessages.getLoggingEvents().get(0).getMessage()).startsWith("JWT-050");
        Assertions.assertThat(TestLogMessages.getLoggingEvents().get(0).getArguments().get(0)).isEqualTo("Request was denied access by RolesFilter because there was no role matching (required role(s) 'role').");

    }


    @Test
    void filter_noAuthentication() {
        TestLogMessages.init();
        activateTracing();

        RolesAllowedFilter rolesAllowedFilter = new RolesAllowedFilter(new String[]{"role"});

        Mockito.when(containerRequestContextMock.getSecurityContext()).thenReturn(new TestSecurityContext(false, ""));
        Assertions.assertThatThrownBy(() -> rolesAllowedFilter.filter(containerRequestContextMock))
                .isInstanceOf(NotAuthorizedException.class);

        Assertions.assertThat(TestLogMessages.getLoggingEvents()).hasSize(1);

        Assertions.assertThat(TestLogMessages.getLoggingEvents().get(0).getMessage()).startsWith("JWT-050");
        Assertions.assertThat(TestLogMessages.getLoggingEvents().get(0).getArguments().get(0)).isEqualTo("Request was denied access by RolesFilter because there was no Bearer Token.");

    }

    private void activateTracing() {
        LogTracingHelper.getInstance().storeLogTracingActive("/root", true);

        URI uri = URI.create("http://localhost:8080/root/endpoint");
        Mockito.when(uriInfoMock.getRequestUri()).thenReturn(uri);

        Mockito.when(requestContextMock.getUriInfo()).thenReturn(uriInfoMock);

        LogTracingHelper.getInstance().startTracing(requestContextMock);
    }

    private void noTracing() {
        LogTracingHelper.getInstance().storeLogTracingActive("/root", false);

        URI uri = URI.create("http://localhost:8080/root/endpoint");
        Mockito.when(uriInfoMock.getRequestUri()).thenReturn(uri);

        Mockito.when(requestContextMock.getUriInfo()).thenReturn(uriInfoMock);

        LogTracingHelper.getInstance().startTracing(requestContextMock);
    }

    private static class TestSecurityContext implements SecurityContext {
        private final boolean hasUserPrincipal;
        private final List<String> roles;

        public TestSecurityContext(boolean hasUserPrincipal, String... roles) {
            this.hasUserPrincipal = hasUserPrincipal;
            this.roles = Arrays.asList(roles);
        }

        @Override
        public Principal getUserPrincipal() {
            if (hasUserPrincipal) {
                return () -> "Test";  // new Principal and getName() returns "Test".
            } else {
                return null;
            }
        }

        @Override
        public boolean isUserInRole(String role) {
            return roles.contains(role);
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Override
        public String getAuthenticationScheme() {
            return null;
        }
    }
}