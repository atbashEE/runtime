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

import be.atbash.runtime.security.jwt.module.LogTracingHelper;
import jakarta.annotation.Priority;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.SecurityContext;

import java.util.Arrays;
import java.util.List;

/**
 * Based on SmallRye JWT.
 */
@Priority(Priorities.AUTHORIZATION)
public class RolesAllowedFilter implements ContainerRequestFilter {

    private final List<String> allowedRoles;
    private final boolean allRolesAllowed;

    public RolesAllowedFilter(String[] allowedRoles) {
        this.allowedRoles = Arrays.asList(allowedRoles);
        allRolesAllowed = this.allowedRoles.contains("*");
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        SecurityContext securityContext = requestContext.getSecurityContext();
        boolean isForbidden;
        if (allRolesAllowed) {
            isForbidden = securityContext.getUserPrincipal() == null;
        } else {
            isForbidden = allowedRoles.stream().noneMatch(securityContext::isUserInRole);
        }
        LogTracingHelper logTracingHelper = LogTracingHelper.getInstance();
        if (isForbidden) {
            if (requestContext.getSecurityContext().getUserPrincipal() == null) {

                logTracingHelper.logTraceMessage("Request was denied access by RolesFilter because there was no Bearer Token.");
                throw new NotAuthorizedException("Bearer");
            } else {
                logTracingHelper.logTraceMessage("Request was denied access by RolesFilter because there was no role matching (required role(s) '%s').", () -> new Object[]{getRolesForLogging()});
                throw new ForbiddenException();
            }
        } else {
            logTracingHelper.logTraceMessage("Request is permitted as there was a matching role (required role(s) '%s').", () -> new Object[]{getRolesForLogging()});
        }
    }

    private String getRolesForLogging() {
        if (allRolesAllowed) {
            return "Any role allowed (a role contained '*')";
        } else {
            return String.join(",", allowedRoles);
        }
    }
}
