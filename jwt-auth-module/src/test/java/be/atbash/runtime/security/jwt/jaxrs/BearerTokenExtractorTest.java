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
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class BearerTokenExtractorTest {

    @Mock
    private ContainerRequestContext requestContextMock;

    @Test
    void getBearerToken_justHeader() {
        Mockito.when(requestContextMock.getHeaderString("Authorization")).thenReturn("Bearer theToken");
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        BearerTokenExtractor extractor = new BearerTokenExtractor(requestContextMock, authContextInfo);

        String bearerToken = extractor.getBearerToken();
        Assertions.assertThat(bearerToken).isEqualTo("theToken");
    }

    @Test
    void getBearerToken_justCookie() {
        Map<String, Cookie> cookies = new HashMap<>();
        cookies.put("Bearer", new Cookie("Bearer", "theTokenFromCookie"));
        Mockito.when(requestContextMock.getCookies()).thenReturn(cookies);
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        BearerTokenExtractor extractor = new BearerTokenExtractor(requestContextMock, authContextInfo);

        String bearerToken = extractor.getBearerToken();
        Assertions.assertThat(bearerToken).isEqualTo("theTokenFromCookie");
    }

    @Test
    void getBearerToken_forceHeader() {
        Mockito.when(requestContextMock.getHeaderString("Authorization")).thenReturn("Bearer theToken");

        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setTokenHeader("Authorization");
        BearerTokenExtractor extractor = new BearerTokenExtractor(requestContextMock, authContextInfo);

        String bearerToken = extractor.getBearerToken();
        Assertions.assertThat(bearerToken).isEqualTo("theToken");
    }

    @Test
    void getBearerToken_TryCookie() {
        Map<String, Cookie> cookies = new HashMap<>();
        cookies.put("Bearer", new Cookie("Bearer", "theTokenFromCookie"));
        Mockito.when(requestContextMock.getCookies()).thenReturn(cookies);

        // Lenient to make it explicit
        Mockito.lenient().when(requestContextMock.getHeaderString("Authorization")).thenReturn("Bearer theToken");

        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setTokenHeader("Cookie");
        BearerTokenExtractor extractor = new BearerTokenExtractor(requestContextMock, authContextInfo);

        String bearerToken = extractor.getBearerToken();
        Assertions.assertThat(bearerToken).isEqualTo("theTokenFromCookie");

        Mockito.verify(requestContextMock, Mockito.never()).getHeaderString("Authorization");
    }

    @Test
    void getBearerToken_nothingAvailable() {
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        BearerTokenExtractor extractor = new BearerTokenExtractor(requestContextMock, authContextInfo);

        String bearerToken = extractor.getBearerToken();
        Assertions.assertThat(bearerToken).isNull();
    }

    @Test
    void getBearerToken_preferHeader() {
        Mockito.when(requestContextMock.getHeaderString("Authorization")).thenReturn("Bearer theToken");

        Map<String, Cookie> cookies = new HashMap<>();
        cookies.put("Bearer", new Cookie("Bearer", "theTokenFromCookie"));
        // lenient on purpose to differ from getBearerToken_justHeader() test
        Mockito.lenient().when(requestContextMock.getCookies()).thenReturn(cookies);

        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        BearerTokenExtractor extractor = new BearerTokenExtractor(requestContextMock, authContextInfo);

        String bearerToken = extractor.getBearerToken();
        Assertions.assertThat(bearerToken).isEqualTo("theToken");
    }

    @Test
    void getBearerToken_specifyCookieName() {
        Map<String, Cookie> cookies = new HashMap<>();
        cookies.put("custom", new Cookie("custom", "theTokenFromCustomCookie"));
        Mockito.when(requestContextMock.getCookies()).thenReturn(cookies);

        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setTokenCookie("custom");

        BearerTokenExtractor extractor = new BearerTokenExtractor(requestContextMock, authContextInfo);

        String bearerToken = extractor.getBearerToken();
        Assertions.assertThat(bearerToken).isEqualTo("theTokenFromCustomCookie");
    }

}