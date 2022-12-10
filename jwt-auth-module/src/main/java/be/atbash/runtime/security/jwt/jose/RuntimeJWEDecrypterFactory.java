package be.atbash.runtime.security.jwt.jose;

import be.atbash.ee.security.octopus.nimbus.jose.JOSEException;
import be.atbash.ee.security.octopus.nimbus.jose.crypto.factories.DefaultJWEDecrypterFactory;
import be.atbash.ee.security.octopus.nimbus.jwt.jwe.JWEDecrypter;
import be.atbash.ee.security.octopus.nimbus.jwt.jwe.JWEHeader;
import be.atbash.runtime.security.jwt.principal.JWTAuthContextInfo;

import java.security.Key;

public class RuntimeJWEDecrypterFactory extends DefaultJWEDecrypterFactory {

    private final JWTAuthContextInfo authContextInfo;

    public RuntimeJWEDecrypterFactory(JWTAuthContextInfo authContextInfo) {
        this.authContextInfo = authContextInfo;
    }

    @Override
    public JWEDecrypter createJWEDecrypter(JWEHeader header, Key key) {
        // Check if the algorithms that can be used are restricted.
        if (!authContextInfo.getEncryptionAlgorithms().isEmpty()) {
            if (!authContextInfo.getEncryptionAlgorithms().contains(header.getAlgorithm())) {
                throw new JOSEException("Unsupported JWE algorithm: " + header.getAlgorithm());
            }
        }
        return super.createJWEDecrypter(header, key);
    }
}
