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
import org.eclipse.microprofile.jwt.Claims;

import java.text.ParseException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A default implementation of JWTCallerPrincipal that wraps the jose4j JwtClaims.
 *
 * @see JWTClaimsSet
 */
public class DefaultJWTCallerPrincipal extends JWTCallerPrincipal {
    private final JWTClaimsSet claimsSet;
    private final JWTAuthContextInfo authContextInfo;

    /**
     * Create the DefaultJWTCallerPrincipal from the parsed JWT token and the extracted principal name
     *
     * @param rawToken        - raw token value
     * @param claimsSet       - JWT claims set
     * @param authContextInfo - The Context information
     */
    public DefaultJWTCallerPrincipal(String rawToken, JWTClaimsSet claimsSet, JWTAuthContextInfo authContextInfo) {
        super(rawToken);
        this.claimsSet = claimsSet;
        this.authContextInfo = authContextInfo;
    }

    @Override
    public Set<String> getAudience() {
        // This is to return null when aud claim did not exist in token.
        Set<String> audSet = null;

        List<String> audiences = claimsSet.getAudience();
        if (!audiences.isEmpty()) {
            audSet = new HashSet<>(audiences);
        }
        return audSet;
    }

    @Override
    public Set<String> getGroups() {
        HashSet<String> groups = new HashSet<>();
        try {
            List<String> globalGroups = claimsSet.getStringListClaim(authContextInfo.getGroupsClaimName());
            if (globalGroups != null) {
                groups.addAll(globalGroups);
            }
        } catch (java.text.ParseException e) {
            throw new RuntimeException(e);  // FIXME a proper exception
        }
        return groups;
    }

    @Override
    protected Collection<String> doGetClaimNames() {
        return claimsSet.getClaims().keySet();
    }

    @Override
    protected Object getClaimValue(String claimName) {
        Claims claimType = getClaimType(claimName);
        Object claim;

        switch (claimType) {
            case exp:
            case iat:
            case auth_time:
            case nbf:
            case updated_at:
                try {
                    claim = claimsSet.getLongClaim(claimType.name());
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                break;
            case groups:
                claim = getGroups();
                break;
            case aud:
                claim = getAudience();
                break;
            case UNKNOWN:
                claim = claimsSet.getClaim(claimName);
                break;
            default:
                claim = claimsSet.getClaim(claimType.name());
        }
        return claim;
    }

}
