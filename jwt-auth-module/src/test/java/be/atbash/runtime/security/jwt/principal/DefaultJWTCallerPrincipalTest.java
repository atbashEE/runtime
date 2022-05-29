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
package be.atbash.runtime.security.jwt.principal;

import be.atbash.ee.security.octopus.nimbus.jwt.JWTClaimsSet;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class DefaultJWTCallerPrincipalTest {

    @Test
    void getGroups() {
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder()
                .claim("customGroupName", List.of("group1", "group2"))
                .build();
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setGroupsClaimName("customGroupName");  // Default value that is set is "groups" but getGroups just takes what is defined.
        DefaultJWTCallerPrincipal callerPrincipal = new DefaultJWTCallerPrincipal("TheRawToken", claimSet, authContextInfo);
        Assertions.assertThat(callerPrincipal.getGroups()).containsOnly("group1", "group2");
    }

    @Test
    void getName_fromUPN() {
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder()
                .subject("principalName")
                .claim("upn", "upnValue")
                .claim("preferred_username", "username")
                .build();
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        DefaultJWTCallerPrincipal callerPrincipal = new DefaultJWTCallerPrincipal("TheRawToken", claimSet, authContextInfo);
        Assertions.assertThat(callerPrincipal.getName()).isEqualTo("upnValue");
    }

    @Test
    void getName_fromUserName() {
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder()
                .subject("principalName")
                .claim("preferred_username", "username")
                .build();
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        DefaultJWTCallerPrincipal callerPrincipal = new DefaultJWTCallerPrincipal("TheRawToken", claimSet, authContextInfo);
        Assertions.assertThat(callerPrincipal.getName()).isEqualTo("username");
    }

    @Test
    void getName_fromSubject() {
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder()
                .subject("principalName")
                .build();
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        DefaultJWTCallerPrincipal callerPrincipal = new DefaultJWTCallerPrincipal("TheRawToken", claimSet, authContextInfo);
        Assertions.assertThat(callerPrincipal.getName()).isEqualTo("principalName");
    }
}