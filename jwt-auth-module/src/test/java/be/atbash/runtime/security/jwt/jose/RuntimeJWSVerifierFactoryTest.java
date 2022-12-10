package be.atbash.runtime.security.jwt.jose;

import be.atbash.ee.security.octopus.keys.selector.AsymmetricPart;
import be.atbash.ee.security.octopus.keys.selector.SelectorCriteria;
import be.atbash.ee.security.octopus.nimbus.jose.JOSEException;
import be.atbash.ee.security.octopus.nimbus.jwt.jws.JWSAlgorithm;
import be.atbash.ee.security.octopus.nimbus.jwt.jws.JWSHeader;
import be.atbash.ee.security.octopus.nimbus.jwt.jws.JWSVerifier;
import be.atbash.runtime.security.jwt.principal.JWTAuthContextInfo;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.util.ArrayList;
import java.util.List;

class RuntimeJWSVerifierFactoryTest {

    @Test
    void createJWSVerifier_noRestriction() {
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setSignatureAlgorithms(new ArrayList<>());
        RuntimeJWSVerifierFactory factory = new RuntimeJWSVerifierFactory(authContextInfo);

        JWSHeader header = new JWSHeader(JWSAlgorithm.RS256);
        SelectorCriteria criteria = SelectorCriteria.newBuilder()
                .withAsymmetricPart(AsymmetricPart.PUBLIC)
                .build();
        Key key = TestKeys.generateRSAKeys("kid", criteria).get(0).getKey();

        JWSVerifier verifier = factory.createJWSVerifier(header, key);
        Assertions.assertThat(verifier).isNotNull();

    }

    @Test
    void createJWSVerifier_SingleRestriction() {
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setSignatureAlgorithms(List.of(JWSAlgorithm.RS384));
        RuntimeJWSVerifierFactory factory = new RuntimeJWSVerifierFactory(authContextInfo);

        JWSHeader header = new JWSHeader(JWSAlgorithm.RS256);
        SelectorCriteria criteria = SelectorCriteria.newBuilder()
                .withAsymmetricPart(AsymmetricPart.PUBLIC)
                .build();
        Key key = TestKeys.generateRSAKeys("kid", criteria).get(0).getKey();

        Assertions.assertThatThrownBy(() ->
                        factory.createJWSVerifier(header, key))
                .isInstanceOf(JOSEException.class)
                .hasMessage("Unsupported JWS algorithm: RS256");


    }

    @Test
    void createJWSVerifier_MultipleRestrictions() {
        JWTAuthContextInfo authContextInfo = new JWTAuthContextInfo();
        authContextInfo.setSignatureAlgorithms(List.of(JWSAlgorithm.RS256, JWSAlgorithm.RS384));
        RuntimeJWSVerifierFactory factory = new RuntimeJWSVerifierFactory(authContextInfo);

        JWSHeader header = new JWSHeader(JWSAlgorithm.RS256);
        SelectorCriteria criteria = SelectorCriteria.newBuilder()
                .withAsymmetricPart(AsymmetricPart.PUBLIC)
                .build();
        Key key = TestKeys.generateRSAKeys("kid", criteria).get(0).getKey();

        JWSVerifier verifier = factory.createJWSVerifier(header, key);
        Assertions.assertThat(verifier).isNotNull();

    }
}