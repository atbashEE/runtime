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

import be.atbash.ee.security.octopus.nimbus.jwt.jwe.JWEAlgorithm;
import be.atbash.ee.security.octopus.nimbus.jwt.jws.JWSAlgorithm;
import jakarta.enterprise.inject.Vetoed;

import java.util.List;
import java.util.Set;

/**
 * The public key and expected issuer needed to validate a token.
 */
@Vetoed
public class JWTAuthContextInfo {

    private List<String> issuedBy;
    private int expGracePeriodSecs = 60;
    private List<String> publicKeyLocation;
    private String publicKeyContent;
    private List<String> decryptionKeyLocation;

    private int keysRefreshInterval;
    private int forcedKeysRefreshInterval;
    private String tokenHeader;
    private String tokenCookie;
    private String groupsClaimName;
    private List<JWSAlgorithm> signatureAlgorithms;
    private List<JWEAlgorithm> encryptionAlgorithms;
    private Set<String> expectedAudience;
    private Set<String> requiredClaims;

    public List<String> getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(List<String> issuedBy) {
        this.issuedBy = issuedBy;
    }

    public int getExpGracePeriodSecs() {
        return expGracePeriodSecs;
    }

    public void setExpGracePeriodSecs(int expGracePeriodSecs) {
        this.expGracePeriodSecs = expGracePeriodSecs;
    }

    public List<String> getPublicKeyLocation() {
        return this.publicKeyLocation;
    }

    public void setPublicKeyLocation(List<String> publicKeyLocation) {
        this.publicKeyLocation = publicKeyLocation;
    }

    public List<String> getDecryptionKeyLocation() {
        return decryptionKeyLocation;
    }

    public boolean isJWERequired() {
        return !decryptionKeyLocation.isEmpty();
    }

    public void setDecryptionKeyLocation(List<String> keyLocation) {
        this.decryptionKeyLocation = keyLocation;
    }

    public String getPublicKeyContent() {
        return this.publicKeyContent;
    }

    public void setPublicKeyContent(String publicKeyContent) {
        this.publicKeyContent = publicKeyContent;
    }

    public int getKeysRefreshInterval() {
        return keysRefreshInterval;
    }

    public void setKeysRefreshInterval(int keysRefreshInterval) {
        this.keysRefreshInterval = keysRefreshInterval;
    }

    public int getForcedKeysRefreshInterval() {
        return forcedKeysRefreshInterval;
    }

    public void setForcedKeysRefreshInterval(int forcedKeysRefreshInterval) {
        this.forcedKeysRefreshInterval = forcedKeysRefreshInterval;
    }

    public String getTokenHeader() {
        return tokenHeader;
    }

    public void setTokenHeader(String tokenHeader) {
        this.tokenHeader = tokenHeader;
    }

    public String getTokenCookie() {
        return tokenCookie;
    }

    public void setTokenCookie(String tokenCookie) {
        this.tokenCookie = tokenCookie;
    }

    public String getGroupsClaimName() {
        return groupsClaimName;
    }

    public void setGroupsClaimName(String groupsClaimName) {
        this.groupsClaimName = groupsClaimName;
    }

    public Set<String> getExpectedAudience() {
        return expectedAudience;
    }

    public void setExpectedAudience(Set<String> expectedAudience) {
        this.expectedAudience = expectedAudience;
    }

    public List<JWSAlgorithm> getSignatureAlgorithms() {
        return signatureAlgorithms;
    }

    public void setSignatureAlgorithms(List<JWSAlgorithm> signatureAlgorithms) {
        this.signatureAlgorithms = signatureAlgorithms;
    }

    public List<JWEAlgorithm> getEncryptionAlgorithms() {
        return encryptionAlgorithms;
    }

    public void setEncryptionAlgorithms(List<JWEAlgorithm> encryptionAlgorithms) {
        this.encryptionAlgorithms = encryptionAlgorithms;
    }

    public Set<String> getRequiredClaims() {
        return requiredClaims;
    }

    public void setRequiredClaims(Set<String> requiredClaims) {
        this.requiredClaims = requiredClaims;
    }

    @Override
    public String toString() {
        return "JWTAuthContextInfo{" +
                ", issuedBy='" + issuedBy + '\'' +
                ", expGracePeriodSecs=" + expGracePeriodSecs +
                ", publicKeyLocation='" + publicKeyLocation + '\'' +
                ", publicKeyContent='" + publicKeyContent + '\'' +
                ", decryptionKeyLocation='" + decryptionKeyLocation + '\'' +
                ", keysRefreshInterval=" + keysRefreshInterval +
                ", forcedKeysRefreshInterval=" + forcedKeysRefreshInterval +
                ", tokenHeader='" + tokenHeader + '\'' +
                ", tokenCookie='" + tokenCookie + '\'' +
                ", defaultGroupsClaim='" + groupsClaimName + '\'' +
                ", signatureAlgorithm=" + signatureAlgorithms +
                ", expectedAudience=" + expectedAudience +
                '}';
    }
}
