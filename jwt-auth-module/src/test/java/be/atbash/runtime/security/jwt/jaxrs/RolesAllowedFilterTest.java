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

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.SecurityContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;


@ExtendWith(MockitoExtension.class)
class RolesAllowedFilterTest {

    @Mock
    private ContainerRequestContext containerRequestContextMock;

    @Test
    void filter() {
        RolesAllowedFilter rolesAllowedFilter = new RolesAllowedFilter(new String[]{"role"});

        Mockito.when(containerRequestContextMock.getSecurityContext()).thenReturn(new TestSecurityContext(true, "role"));
        rolesAllowedFilter.filter(containerRequestContextMock);
    }

    @Test
    void filter_multiple() {
        RolesAllowedFilter rolesAllowedFilter = new RolesAllowedFilter(new String[]{"role1","role2"});

        Mockito.when(containerRequestContextMock.getSecurityContext()).thenReturn(new TestSecurityContext(true, "role2","role3"));
        rolesAllowedFilter.filter(containerRequestContextMock);
    }

    @Test
    void filter_any() {
        RolesAllowedFilter rolesAllowedFilter = new RolesAllowedFilter(new String[]{"*"});

        Mockito.when(containerRequestContextMock.getSecurityContext()).thenReturn(new TestSecurityContext(true, "role"));
        rolesAllowedFilter.filter(containerRequestContextMock);
    }

    @Test
    void filter_missingRole() {
        RolesAllowedFilter rolesAllowedFilter = new RolesAllowedFilter(new String[]{"role"});

        Mockito.when(containerRequestContextMock.getSecurityContext()).thenReturn(new TestSecurityContext(true, "JUnit"));
        Assertions.assertThatThrownBy(() -> rolesAllowedFilter.filter(containerRequestContextMock))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void filter_noAuthentication() {
        RolesAllowedFilter rolesAllowedFilter = new RolesAllowedFilter(new String[]{"role"});

        Mockito.when(containerRequestContextMock.getSecurityContext()).thenReturn(new TestSecurityContext(false, ""));
        Assertions.assertThatThrownBy(() -> rolesAllowedFilter.filter(containerRequestContextMock))
                .isInstanceOf(NotAuthorizedException.class);
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