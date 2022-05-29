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

import be.atbash.ee.security.octopus.nimbus.jwt.CommonJWTHeader;
import be.atbash.ee.security.octopus.nimbus.jwt.JWTClaimsSet;
import be.atbash.ee.security.octopus.nimbus.jwt.jws.JWSAlgorithm;
import be.atbash.ee.security.octopus.nimbus.jwt.jws.JWSHeader;
import be.atbash.runtime.core.data.util.SystemPropertyUtil;
import be.atbash.util.TestReflectionUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

class MPBearerTokenVerifierTest {

    @AfterEach
    public void cleanup() throws NoSuchFieldException {
        System.clearProperty("atbash.runtime.tck");
        System.clearProperty("atbash.runtime.tck.jwt");
        // reset the JVM Singleton between each run !
        Map<?, ?> cache = TestReflectionUtils.getValueOf(SystemPropertyUtil.getInstance(), "resultCache");
        cache.clear();

    }

    @Test
    void verify() {
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setIssuedBy(List.of("JUnit"));
        authContextInfo.setExpectedAudience(Collections.emptySet());
        authContextInfo.setRequiredClaims(Collections.emptySet());

        MPBearerTokenVerifier verifier = new MPBearerTokenVerifier(authContextInfo);
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder()
                .issuer("JUnit")
                .subject("Subject")
                .expirationTime(Duration.of(10, ChronoUnit.SECONDS))
                .build();
        CommonJWTHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .build();
        boolean valid = verifier.verify(header, claimSet);
        Assertions.assertThat(valid).isTrue();

    }

    @Test
    void verify_expInPast() {
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setIssuedBy(List.of("JUnit"));
        authContextInfo.setExpectedAudience(Collections.emptySet());
        authContextInfo.setRequiredClaims(Collections.emptySet());

        LocalDateTime exp = LocalDateTime.now().minusSeconds(1);
        MPBearerTokenVerifier verifier = new MPBearerTokenVerifier(authContextInfo);
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder()
                .issuer("JUnit")
                .subject("Subject")
                .expirationTime(exp)
                .build();
        CommonJWTHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .build();
        boolean valid = verifier.verify(header, claimSet);
        Assertions.assertThat(valid).isTrue();

    }

    @Test
    void verify_noExp() {
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setIssuedBy(List.of("JUnit"));
        authContextInfo.setExpectedAudience(Collections.emptySet());
        authContextInfo.setRequiredClaims(Collections.emptySet());

        MPBearerTokenVerifier verifier = new MPBearerTokenVerifier(authContextInfo);
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder()
                .issuer("JUnit")
                .subject("Subject")
                .build();
        CommonJWTHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .build();
        boolean valid = verifier.verify(header, claimSet);
        Assertions.assertThat(valid).isFalse();

    }

    @Test
    void verify_allowUPN() {
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setIssuedBy(List.of("JUnit"));
        authContextInfo.setExpectedAudience(Collections.emptySet());
        authContextInfo.setRequiredClaims(Collections.emptySet());

        MPBearerTokenVerifier verifier = new MPBearerTokenVerifier(authContextInfo);
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder()
                .issuer("JUnit")
                .claim("upn", "Subject")
                .expirationTime(Duration.of(10, ChronoUnit.SECONDS))
                .build();
        CommonJWTHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .build();
        boolean valid = verifier.verify(header, claimSet);
        Assertions.assertThat(valid).isTrue();

    }


    @Test
    void verify_wrongIssuer() {
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setIssuedBy(List.of("JUnit"));
        authContextInfo.setExpectedAudience(Collections.emptySet());
        authContextInfo.setRequiredClaims(Collections.emptySet());

        MPBearerTokenVerifier verifier = new MPBearerTokenVerifier(authContextInfo);
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder()
                .issuer("someone")
                .subject("Subject")
                .expirationTime(Duration.of(10, ChronoUnit.SECONDS))
                .build();
        CommonJWTHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .build();
        boolean valid = verifier.verify(header, claimSet);
        Assertions.assertThat(valid).isFalse();

    }

    @Test
    void verify_noIssuer() {
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setIssuedBy(List.of("JUnit"));
        authContextInfo.setExpectedAudience(Collections.emptySet());
        authContextInfo.setRequiredClaims(Collections.emptySet());

        MPBearerTokenVerifier verifier = new MPBearerTokenVerifier(authContextInfo);
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder()
                .subject("Subject")
                .expirationTime(Duration.of(10, ChronoUnit.SECONDS))
                .build();
        CommonJWTHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .build();
        boolean valid = verifier.verify(header, claimSet);
        Assertions.assertThat(valid).isFalse();

    }

    @Test
    void verify_TCKMode() {
        System.setProperty("atbash.runtime.tck.jwt", "true");
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setIssuedBy(List.of("JUnit"));
        authContextInfo.setExpectedAudience(Collections.emptySet());
        authContextInfo.setRequiredClaims(Collections.emptySet());

        MPBearerTokenVerifier verifier = new MPBearerTokenVerifier(authContextInfo);
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder()
                .issuer("JUnit")
                .subject("Subject")
                .issueTime(new Date())
                .jwtID("unique")
                .expirationTime(Duration.of(10, ChronoUnit.SECONDS))
                .build();
        CommonJWTHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .build();
        boolean valid = verifier.verify(header, claimSet);
        Assertions.assertThat(valid).isTrue();

    }

    @Test
    void verify_TCKMode_missing_jti() {
        System.setProperty("atbash.runtime.tck.jwt", "true");
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setIssuedBy(List.of("JUnit"));
        authContextInfo.setExpectedAudience(Collections.emptySet());
        authContextInfo.setRequiredClaims(Collections.emptySet());

        MPBearerTokenVerifier verifier = new MPBearerTokenVerifier(authContextInfo);
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder()
                .issuer("JUnit")
                .subject("Subject")
                .issueTime(new Date())
                .expirationTime(Duration.of(10, ChronoUnit.SECONDS))
                .build();
        CommonJWTHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .build();
        boolean valid = verifier.verify(header, claimSet);
        Assertions.assertThat(valid).isFalse();

    }

    @Test
    void verify_TCKMode_missing_iat() {
        System.setProperty("atbash.runtime.tck.jwt", "true");
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setIssuedBy(List.of("JUnit"));
        authContextInfo.setExpectedAudience(Collections.emptySet());
        authContextInfo.setRequiredClaims(Collections.emptySet());

        MPBearerTokenVerifier verifier = new MPBearerTokenVerifier(authContextInfo);
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder()
                .issuer("JUnit")
                .subject("Subject")
                .jwtID("unique")
                .expirationTime(Duration.of(10, ChronoUnit.SECONDS))
                .build();
        CommonJWTHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .build();
        boolean valid = verifier.verify(header, claimSet);
        Assertions.assertThat(valid).isFalse();

    }

    @Test
    void verify_noClaims() {
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setIssuedBy(List.of("JUnit"));
        authContextInfo.setExpectedAudience(Collections.emptySet());
        authContextInfo.setRequiredClaims(Collections.emptySet());

        MPBearerTokenVerifier verifier = new MPBearerTokenVerifier(authContextInfo);
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder().build();
        CommonJWTHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .build();
        boolean valid = verifier.verify(header, claimSet);
        Assertions.assertThat(valid).isFalse();
    }

    @Test
    void verify_checkAdditionalClaims() {
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setIssuedBy(List.of("JUnit"));
        authContextInfo.setExpectedAudience(Collections.emptySet());
        authContextInfo.setRequiredClaims(Set.of("claim"));

        MPBearerTokenVerifier verifier = new MPBearerTokenVerifier(authContextInfo);
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder()
                .issuer("JUnit")
                .subject("Subject")
                .claim("claim", "value")
                .expirationTime(Duration.of(10, ChronoUnit.SECONDS))
                .build();
        CommonJWTHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .build();
        boolean valid = verifier.verify(header, claimSet);
        Assertions.assertThat(valid).isTrue();

    }

    @Test
    void verify_checkAdditionalClaims_missing() {
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setIssuedBy(List.of("JUnit"));
        authContextInfo.setExpectedAudience(Collections.emptySet());
        authContextInfo.setRequiredClaims(Set.of("claim"));

        MPBearerTokenVerifier verifier = new MPBearerTokenVerifier(authContextInfo);
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder()
                .issuer("JUnit")
                .subject("Subject")
                .expirationTime(Duration.of(10, ChronoUnit.SECONDS))
                .build();
        CommonJWTHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .build();
        boolean valid = verifier.verify(header, claimSet);
        Assertions.assertThat(valid).isFalse();

    }

    @Test
    void verify_checkAudience() {
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setIssuedBy(List.of("JUnit"));
        authContextInfo.setExpectedAudience(Set.of("Audience", "otherAudience"));
        authContextInfo.setRequiredClaims(Collections.emptySet());

        MPBearerTokenVerifier verifier = new MPBearerTokenVerifier(authContextInfo);
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder()
                .issuer("JUnit")
                .subject("Subject")
                .audience("Audience")
                .expirationTime(Duration.of(10, ChronoUnit.SECONDS))
                .build();
        CommonJWTHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .build();
        boolean valid = verifier.verify(header, claimSet);
        Assertions.assertThat(valid).isTrue();

    }

    @Test
    void verify_missingAudience() {
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setIssuedBy(List.of("JUnit"));
        authContextInfo.setExpectedAudience(Set.of("audience"));
        authContextInfo.setRequiredClaims(Collections.emptySet());

        MPBearerTokenVerifier verifier = new MPBearerTokenVerifier(authContextInfo);
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder()
                .issuer("JUnit")
                .subject("Subject")
                .expirationTime(Duration.of(10, ChronoUnit.SECONDS))
                .build();
        CommonJWTHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .build();
        boolean valid = verifier.verify(header, claimSet);
        Assertions.assertThat(valid).isFalse();

    }

    @Test
    void verify_wrongAudience() {
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setIssuedBy(List.of("JUnit"));
        authContextInfo.setExpectedAudience(Set.of("aud1", "aud2"));
        authContextInfo.setRequiredClaims(Collections.emptySet());

        MPBearerTokenVerifier verifier = new MPBearerTokenVerifier(authContextInfo);
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder()
                .issuer("JUnit")
                .subject("Subject")
                .audience("something-else")
                .expirationTime(Duration.of(10, ChronoUnit.SECONDS))
                .build();
        CommonJWTHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .build();
        boolean valid = verifier.verify(header, claimSet);
        Assertions.assertThat(valid).isFalse();

    }


}