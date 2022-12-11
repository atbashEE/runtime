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
package be.atbash.runtime.security.jwt;


import be.atbash.config.exception.ConfigurationException;
import be.atbash.ee.security.octopus.nimbus.jwt.jwe.JWEAlgorithm;
import be.atbash.ee.security.octopus.nimbus.jwt.jws.JWSAlgorithm;
import be.atbash.ee.security.octopus.util.PeriodUtil;
import be.atbash.runtime.core.data.util.ResourceReader;
import be.atbash.runtime.core.data.util.SystemPropertyUtil;
import be.atbash.runtime.security.jwt.principal.JWTAuthContextInfo;
import be.atbash.util.resource.ResourceUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.config.Names;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A CDI provider for the JWTAuthContextInfo that obtains the necessary information from
 * MP config properties.
 * <p>
 * Based on SmallRye JWT
 */
@ApplicationScoped
public class JWTAuthContextInfoProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(JWTAuthContextInfoProvider.class);

    private static final String DEFAULT_COOKIE_NAME = "Bearer";
    private static final String NONE = "NONE";

    private JWTAuthContextInfo authContextInfo;

    // The MP-JWT spec defined configuration properties

    /**
     * @since 1.1
     */
    @Inject
    @ConfigProperty(name = Names.VERIFIER_PUBLIC_KEY, defaultValue = NONE)
    private String mpJwtPublicKey;

    /**
     * @since 1.2
     */
    @Inject
    @ConfigProperty(name = Names.VERIFIER_PUBLIC_KEY_ALGORITHM)
    private Optional<List<JWSAlgorithm>> mpJwtPublicKeyAlgorithms;

    @Inject
    @ConfigProperty(name = Names.DECRYPTOR_KEY_ALGORITHM)
    private Optional<List<JWEAlgorithm>> mpJweDecryptorKeyAlgorithms;

    /**
     * @since 1.1
     */
    @Inject
    @ConfigProperty(name = Names.ISSUER, defaultValue = NONE)
    private List<String> mpJwtIssuer;

    /**
     * @since 1.1
     */
    @Inject
    @ConfigProperty(name = Names.VERIFIER_PUBLIC_KEY_LOCATION, defaultValue = NONE)
    private List<String> mpJwtLocation;

    /**
     * @since 1.2
     */
    @Inject
    @ConfigProperty(name = Names.DECRYPTOR_KEY_LOCATION, defaultValue = NONE)
    private List<String> mpJwtDecryptKeyLocation;

    /**
     * Is the Token taken from the Header (Authorization) or a Cookie.
     * If not specified, cookie tried and then header.
     */
    @Inject
    @ConfigProperty(name = Names.TOKEN_HEADER)
    private Optional<String> mpJwtTokenHeader;

    /**
     * @since 1.2
     */
    @Inject
    @ConfigProperty(name = Names.TOKEN_COOKIE)
    private Optional<String> mpJwtTokenCookie;

    /**
     * @since 1.2
     */
    @Inject
    @ConfigProperty(name = Names.AUDIENCES)
    Optional<Set<String>> mpJwtVerifyAudiences;

    @Inject
    @ConfigProperty(name = Names.CLOCK_SKEW, defaultValue = "60")
    private int mpExpGracePeriodSecs;

    @Inject
    @ConfigProperty(name = Names.TOKEN_AGE, defaultValue = "-1")
    private int mpTokenAgeSecs;

    // Atbash specific properties


    /**
     * Default groups claim value. This property can be used to support the JWT tokens without a 'groups' claim.
     */
    @Inject
    @ConfigProperty(name = "atbash.jwt.claims.groups")
    private Optional<String> defaultGroupsClaim;


    /**
     * Interval for the keys from the locations.  The format is
     * <p>
     * <v><unit>
     * <p>
     * * v : A positive integral number
     * * unit : s (seconds), m (minutes) or h (hours)
     * <p>
     * Default value is 24 hours
     */
    @Inject
    @ConfigProperty(name = "atbash.jwt.keys.refresh-interval", defaultValue = "24h")
    private String keysRefreshInterval;

    /**
     * When the key is not found, the locations are reread to see if the information is updated with rotating
     * key information.  To avoid constant reloading by malicious sources, this forced refresh is limited to the period specified
     * in this parameter.  The format is defined as
     * <p>
     * <v><unit>
     * <p>
     * * v : A positive integral number
     * * unit : s (seconds), m (minutes) or h (hours)
     * <p>
     * Default value is 30 minutes
     */
    @Inject
    @ConfigProperty(name = "atbash.jwt.keys.forced-refresh-interval", defaultValue = "30m")
    private String forcedKeysRefreshInterval;


    /**
     * List of claim names that must be present in the JWT for it to be valid. The configuration should be specified
     * as a comma-separated list.
     */
    @Inject
    @ConfigProperty(name = "atbash.jwt.required.claims")
    Optional<Set<String>> requiredClaims;

    private JWTAuthContextInfo getOptionalContextInfo() {

        JWTAuthContextInfo contextInfo = new JWTAuthContextInfo();

        // ValidateJWTConfiguration validates if issuer is defined.
        mpJwtIssuer.remove(NONE);
        contextInfo.setIssuedBy(mpJwtIssuer);

        // public Key, content itself or location
        if (!NONE.equals(mpJwtPublicKey)) {
            contextInfo.setPublicKeyContent(mpJwtPublicKey);
            contextInfo.setPublicKeyLocation(Collections.emptyList());
        } else {
            contextInfo.setPublicKeyLocation(defineLocations(mpJwtLocation));
        }

        // Location of the key(s) used to decrypt the JWE
        contextInfo.setDecryptionKeyLocation(defineLocations(mpJwtDecryptKeyLocation));

        // Authorization Header or token?
        boolean tck = SystemPropertyUtil.getInstance().isTck("jwt");
        if (tck) {
            // Force Header when no config value set to be Spec compliant.
            contextInfo.setTokenHeader(mpJwtTokenHeader.orElse("Authorization"));
        } else {
            mpJwtTokenHeader.ifPresent(contextInfo::setTokenHeader);
        }

        // Name of the cookie
        contextInfo.setTokenCookie(mpJwtTokenCookie.orElse(DEFAULT_COOKIE_NAME));

        // Force a verification algorithm from the list?
        contextInfo.setSignatureAlgorithms(mpJwtPublicKeyAlgorithms.orElseGet(Collections::emptyList));

        // Force an encryption algorithm from the list?
        contextInfo.setEncryptionAlgorithms(mpJweDecryptorKeyAlgorithms.orElseGet(Collections::emptyList));

        // Audience
        if (mpJwtVerifyAudiences.isPresent()) {
            contextInfo.setExpectedAudience(mpJwtVerifyAudiences.get());
        } else {
            contextInfo.setExpectedAudience(Collections.emptySet());
        }

        // Grace period for exp, iat and nbf
        contextInfo.setExpGracePeriodSecs(mpExpGracePeriodSecs);

        // max token age in seconds (-1 no checks)
        contextInfo.setIatTokenAgeSecs(mpTokenAgeSecs);

        // Atbash specific
        contextInfo.setGroupsClaimName(defaultGroupsClaim.orElse(Claims.groups.name()));

        contextInfo.setKeysRefreshInterval(defineInterval(keysRefreshInterval, "24h"));
        contextInfo.setForcedKeysRefreshInterval(defineInterval(forcedKeysRefreshInterval, "30m"));

        contextInfo.setRequiredClaims(requiredClaims.orElse(Collections.emptySet()));
        return contextInfo;
    }

    private int defineInterval(String keysRefreshInterval, String defaultValue) {
        int result;
        try {
            result = PeriodUtil.defineSecondsInPeriod(keysRefreshInterval);
        } catch (ConfigurationException e) {
            result = PeriodUtil.defineSecondsInPeriod(defaultValue);
        }
        return result;
    }

    private List<String> defineLocations(List<String> configValue) {
        List<String> result = new ArrayList<>();
        for (String location : configValue) {
            if (!NONE.equals(location)) {
                String resolvedVerifyKeyLocation = location.trim();

                if (resolvedVerifyKeyLocation.startsWith("http")) {
                    // If http, we just accept, no checks.
                    result.add(resolvedVerifyKeyLocation);
                } else {
                    // File or classpath, let us check if it exists.
                    if (ResourceReader.existsResource(resolvedVerifyKeyLocation)) {
                        result.add(resolvedVerifyKeyLocation);
                    } else {
                        // See if we can find it within classpath.
                        String classpathResource = getClassPathResource(resolvedVerifyKeyLocation);
                        if (ResourceReader.existsResource(classpathResource)) {
                            result.add(classpathResource);
                        } else {
                            // Nope, not found, log the message.
                            // ValidateJWTConfiguration makes that deployment fails.
                            LOGGER.atInfo()
                                    .addArgument(resolvedVerifyKeyLocation)
                                    .log("JWT-010");
                        }
                    }
                }
            }
        }
        return result;
    }


    private String getClassPathResource(String keyLocation) {
        if (keyLocation.startsWith("/")) {
            keyLocation = keyLocation.substring(1);
        }
        return ResourceUtil.CLASSPATH_PREFIX + keyLocation;
    }

    //@Produces

    public JWTAuthContextInfo getContextInfo() {
        if (authContextInfo == null) {
            authContextInfo = getOptionalContextInfo();
        }
        return authContextInfo;
    }
}
