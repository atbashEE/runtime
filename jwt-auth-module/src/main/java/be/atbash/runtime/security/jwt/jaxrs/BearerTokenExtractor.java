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

import be.atbash.runtime.security.jwt.principal.JWTAuthContextInfo;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Cookie;

class BearerTokenExtractor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String COOKIE_HEADER = "Cookie";
    private static final String BEARER = "Bearer ";


    private final ContainerRequestContext requestContext;
    private final JWTAuthContextInfo authContextInfo;

    BearerTokenExtractor(ContainerRequestContext requestContext, JWTAuthContextInfo authContextInfo) {
        this.requestContext = requestContext;
        this.authContextInfo = authContextInfo;
    }

    /**
     * Find a JWT Bearer token in the request by referencing the configurations
     * found in the {@link JWTAuthContextInfo}. The resulting token may be found
     * in a cookie or another HTTP header, either explicitly configured or the
     * default 'Authorization' header.
     *
     * @return a JWT Bearer token or null if not found
     */
    public String getBearerToken() {
        String tokenHeaderName = authContextInfo.getTokenHeader();

        String bearerValue;

        if (COOKIE_HEADER.equals(tokenHeaderName)) {
            bearerValue = tryCookieThenHeader();
        } else if (AUTHORIZATION_HEADER.equals(tokenHeaderName)) {
            bearerValue = getBearerTokenAuthHeader();
        } else {
            bearerValue = tryHeaderThenCookie();
        }

        return bearerValue;
    }

    private String tryCookieThenHeader() {
        String bearerValue;
        String intermediateBearerValue = getBearerTokenCookie();
        if (intermediateBearerValue == null) {
            // No cookie, try the Header.
            bearerValue = getBearerTokenAuthHeader();
        } else {
            bearerValue = intermediateBearerValue;
        }
        return bearerValue;
    }

    private String tryHeaderThenCookie() {
        String bearerValue;
        String intermediateBearerValue = getBearerTokenAuthHeader();
        if (intermediateBearerValue == null) {
            // No header, try the Cookie.
            bearerValue = getBearerTokenCookie();
        } else {
            bearerValue = intermediateBearerValue;
        }
        return bearerValue;
    }

    private String getBearerTokenCookie() {
        String tokenCookieName = authContextInfo.getTokenCookie();

        if (tokenCookieName == null) {
            tokenCookieName = BEARER.trim();
        }

        return getCookieValue(tokenCookieName);
    }

    private String getBearerTokenAuthHeader() {
        String tokenHeader = requestContext.getHeaderString(AUTHORIZATION_HEADER);
        String bearerValue;

        if (tokenHeader != null) {
            bearerValue = getToken(tokenHeader);
        } else {
            bearerValue = null;
        }

        return bearerValue;
    }

    private String getToken(String headerValue) {
        String result = null;
        if (headerValue.startsWith(BEARER)) {
            result = headerValue.substring(BEARER.length());
        }
        return result;
    }

    private String getCookieValue(String cookieName) {
        Cookie tokenCookie = requestContext.getCookies().get(cookieName);

        if (tokenCookie != null) {
            return tokenCookie.getValue();
        }
        return null;
    }
}
