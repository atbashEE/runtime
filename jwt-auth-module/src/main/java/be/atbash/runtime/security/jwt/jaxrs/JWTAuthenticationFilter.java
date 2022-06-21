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
import be.atbash.runtime.security.jwt.JWTAuthContextInfoProvider;
import be.atbash.runtime.security.jwt.inject.PrincipalProducer;
import be.atbash.runtime.security.jwt.module.LogTracingHelper;
import be.atbash.runtime.security.jwt.principal.JWTCallerPrincipal;
import be.atbash.runtime.security.jwt.principal.JWTCallerPrincipalFactory;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.Principal;

/**
 * A JAX-RS ContainerRequestFilter.
 */
@PreMatching
@Priority(Priorities.AUTHENTICATION)
@Provider
public class JWTAuthenticationFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JWTAuthenticationFilter.class);

    @Inject
    private JWTAuthContextInfoProvider authContextInfoProvider;

    @Inject
    private JWTCallerPrincipalFactory jwtParser;

    @Inject
    private PrincipalProducer producer;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LogTracingHelper logTracingHelper = LogTracingHelper.getInstance();
        logTracingHelper.startTracing(requestContext);

        SecurityContext securityContext = requestContext.getSecurityContext();
        Principal principal = securityContext.getUserPrincipal();
        // If principal already a JsonWebToken means the entire authentication is already performed.

        logTracingHelper.logTraceMessage("Received request on %s", () -> new Object[]{requestContext.getUriInfo().getRequestUri().toString()});
        if (!(principal instanceof JsonWebToken)) {

            BearerTokenExtractor extractor = new BearerTokenExtractor(requestContext, authContextInfoProvider.getContextInfo());
            String bearerToken = extractor.getBearerToken();

            logTracingHelper.logTraceMessage("Bearer token '%s'", bearerToken);

            if (bearerToken != null) {
                try {
                    JWTCallerPrincipal callerPrincipal = jwtParser.parse(bearerToken, authContextInfoProvider.getContextInfo());

                    producer.setJsonWebToken(callerPrincipal);

                    logTracingHelper.logTraceMessage("The Token was accepted and has name = '%s' and roles = '%s'", () -> new Object[]{callerPrincipal.getName(), callerPrincipal.getGroups()});

                    // Install the JWT principal as the caller
                    JWTSecurityContext jwtSecurityContext = new JWTSecurityContext(securityContext, callerPrincipal);
                    requestContext.setSecurityContext(jwtSecurityContext);
                } catch (InvalidJWTException | IllegalArgumentException | JOSEException e) {
                    // IllegalArgumentException Not able to determine the format of the provided Bearer token. So not a valid token.
                    // TODO Should we use ContainerRequestContext.abortWith instead of throwing exceptions?
                    throw new NotAuthorizedException(e, "Bearer");
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                    throw new InternalServerErrorException(e);
                }
            }
        }
    }
}
