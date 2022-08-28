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

import be.atbash.ee.security.octopus.jwt.JWTValidationConstant;
import be.atbash.ee.security.octopus.jwt.decoder.JWTVerifier;
import be.atbash.ee.security.octopus.nimbus.jwt.CommonJWTHeader;
import be.atbash.ee.security.octopus.nimbus.jwt.JWTClaimsSet;
import be.atbash.ee.security.octopus.nimbus.jwt.util.DateUtils;
import be.atbash.runtime.core.data.util.SystemPropertyUtil;
import org.slf4j.MDC;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class MPBearerTokenVerifier implements JWTVerifier {


    private final JWTAuthContextInfo authContextInfo;

    public MPBearerTokenVerifier(JWTAuthContextInfo authContextInfo) {

        this.authContextInfo = authContextInfo;
    }

    @Override
    public boolean verify(CommonJWTHeader commonJWTHeader, JWTClaimsSet jwtClaimsSet) {
        // The exp and nbf are validated by DefaultJWTClaimsVerifier.

        // The token issuer must be one of the
        boolean result = jwtClaimsSet.getIssuer() != null && authContextInfo.getIssuedBy().contains(jwtClaimsSet.getIssuer());

        if (!authContextInfo.getExpectedAudience().isEmpty()) {
            if (!checkAudience(authContextInfo.getExpectedAudience(), jwtClaimsSet.getAudience())) {

                // These messages are in function of JWT validation by Atbash Runtime so have slightly narrow meaning of the provided parameters.
                String expectedAud = String.join(",", authContextInfo.getExpectedAudience());
                String tokenAud = String.join(",", jwtClaimsSet.getAudience());
                MDC.put(JWTValidationConstant.JWT_VERIFICATION_FAIL_REASON, String.format("The token did not contain the expected audience. Expected = %s, token = %s", expectedAud, tokenAud));

                result = false;
            }
        }

        // Fixme already done by DefaultJWTClaimsVerifier but do we use grace period?
        Date now = new Date();
        Date exp = jwtClaimsSet.getExpirationTime();
        if (exp == null || !DateUtils.isAfter(exp, now, authContextInfo.getExpGracePeriodSecs())) {
            result = false;
        } else {
            Date nbf = jwtClaimsSet.getNotBeforeTime();
            if (nbf != null && !DateUtils.isBefore(nbf, now, authContextInfo.getExpGracePeriodSecs())) {
                result = false;
            }
        }

        if (SystemPropertyUtil.getInstance().isTck("jwt")) {
            Date iat = jwtClaimsSet.getIssueTime();
            // The check on iat and jti only forced during TCK as these are optional according the JOSE spec.
            if (iat == null || !DateUtils.isBefore(iat, now, authContextInfo.getExpGracePeriodSecs())) {
                MDC.put(JWTValidationConstant.JWT_VERIFICATION_FAIL_REASON, String.format("The token is used before it is issued (iat = %s)", iat));
                result = false;
            }


            String jti = jwtClaimsSet.getJWTID();
            if (jti == null || jti.isBlank()) {
                MDC.put(JWTValidationConstant.JWT_VERIFICATION_FAIL_REASON, "The token has no token id (jti)");

                result = false;
            }
        }

        try {
            String upn = jwtClaimsSet.getStringClaim("upn");
            String subject = jwtClaimsSet.getSubject();
            if (upn == null && subject == null) {
                MDC.put(JWTValidationConstant.JWT_VERIFICATION_FAIL_REASON, "The token has no subject and 'upn' claim");
                result = false;
            }
        } catch (ParseException e) {
            result = false;
        }

        if (!authContextInfo.getRequiredClaims().isEmpty()) {
            for (String requiredClaim : authContextInfo.getRequiredClaims()) {
                if (!jwtClaimsSet.getClaims().containsKey(requiredClaim)) {
                    result = false;
                    MDC.put(JWTValidationConstant.JWT_VERIFICATION_FAIL_REASON, String.format("The token does not contain the custom defined required claim '%s'", requiredClaim));
                    break;
                }
            }
        }

        return result;
    }

    private boolean checkAudience(Set<String> expectedAudience, List<String> tokenAudience) {
        boolean result = false;
        for (String audience : expectedAudience) {
            if (tokenAudience.contains(audience)) {
                // If one of the expected is present in the token, it is OK.
                result = true;
                break;
            }
        }
        return result;
    }
}
