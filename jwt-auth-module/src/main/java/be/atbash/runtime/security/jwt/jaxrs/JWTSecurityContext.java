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

import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.security.Principal;

/**
 * A delegating JAX-RS SecurityContext that provides access to the JsonWebToken.
 * <p>
 * Based on SmallRye JWT.
 */
public class JWTSecurityContext implements SecurityContext {

    private final SecurityContext delegate;
    private final JsonWebToken principal;

    JWTSecurityContext(SecurityContext delegate, JsonWebToken principal) {
        this.delegate = delegate;
        this.principal = principal;
    }

    @Override
    public Principal getUserPrincipal() {
        return principal;
    }

    @Override
    public boolean isUserInRole(String role) {
        return principal.getGroups().contains(role);
    }

    @Override
    public boolean isSecure() {
        return delegate.isSecure();
    }

    @Override
    public String getAuthenticationScheme() {
        return delegate.getAuthenticationScheme();
    }
}