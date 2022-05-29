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

import be.atbash.ee.security.octopus.keys.AtbashKey;
import be.atbash.ee.security.octopus.keys.generator.KeyGenerator;
import be.atbash.ee.security.octopus.keys.generator.RSAGenerationParameters;
import be.atbash.ee.security.octopus.keys.reader.KeyReader;
import be.atbash.ee.security.octopus.keys.selector.AsymmetricPart;
import be.atbash.ee.security.octopus.keys.selector.SelectorCriteria;
import be.atbash.runtime.security.jwt.JWTAuthContextInfoProvider;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
class RuntimeKeyManagerTest {

    public static final String PUBLIC_KEY_PEM = "publicKey.pem";
    @Mock
    private KeyReader keyReaderMock;

    @Mock
    private JWTAuthContextInfoProvider contextInfoProviderMock;

    @InjectMocks
    private RuntimeKeyManager runtimeKeyManager;


    @Test
    void retrieveKeys() {
        JWTAuthContextInfo info = new JWTAuthContextInfo();
        List<String> locations = new ArrayList<>();
        locations.add(PUBLIC_KEY_PEM);
        info.setPublicKeyLocation(locations);

        info.setDecryptionKeyLocation(Collections.emptyList());

        Mockito.when(contextInfoProviderMock.getContextInfo()).thenReturn(info);

        List<AtbashKey> keys = List.of(getPublicKey());
        Mockito.when(keyReaderMock.readKeyResource(PUBLIC_KEY_PEM)).thenReturn(keys);
        SelectorCriteria criteria = SelectorCriteria.newBuilder()
                .withAsymmetricPart(AsymmetricPart.PUBLIC)
                .build();
        List<AtbashKey> atbashKeys = runtimeKeyManager.retrieveKeys(criteria);
        Assertions.assertThat(atbashKeys).hasSize(1);
        Mockito.verify(keyReaderMock, Mockito.times(1)).readKeyResource(PUBLIC_KEY_PEM);
    }


    @Test
    void retrieveKeys_testRefresh() throws InterruptedException {
        JWTAuthContextInfo info = new JWTAuthContextInfo();
        List<String> locations = new ArrayList<>();
        locations.add(PUBLIC_KEY_PEM);
        info.setPublicKeyLocation(locations);

        info.setDecryptionKeyLocation(Collections.emptyList());

        info.setKeysRefreshInterval(1);  // 1 second

        Mockito.when(contextInfoProviderMock.getContextInfo()).thenReturn(info);

        List<AtbashKey> keys = List.of(getPublicKey());
        Mockito.when(keyReaderMock.readKeyResource(PUBLIC_KEY_PEM)).thenReturn(keys);
        SelectorCriteria criteria = SelectorCriteria.newBuilder()
                .withAsymmetricPart(AsymmetricPart.PUBLIC)
                .build();
        List<AtbashKey> atbashKeys = runtimeKeyManager.retrieveKeys(criteria);
        Assertions.assertThat(atbashKeys).hasSize(1);

        TimeUnit.SECONDS.sleep(2);  // Longer then refresh period.
        runtimeKeyManager.retrieveKeys(criteria);  // The keys should be reloaded

        Mockito.verify(keyReaderMock, Mockito.times(2)).readKeyResource(PUBLIC_KEY_PEM);
    }

    @Test
    void retrieveKeys_testRefreshNotTriggered() throws InterruptedException {
        JWTAuthContextInfo info = new JWTAuthContextInfo();
        List<String> locations = new ArrayList<>();
        locations.add(PUBLIC_KEY_PEM);
        info.setPublicKeyLocation(locations);

        info.setDecryptionKeyLocation(Collections.emptyList());

        info.setKeysRefreshInterval(2);  // 2 seconds

        Mockito.when(contextInfoProviderMock.getContextInfo()).thenReturn(info);

        List<AtbashKey> keys = List.of(getPublicKey());
        Mockito.when(keyReaderMock.readKeyResource(PUBLIC_KEY_PEM)).thenReturn(keys);
        SelectorCriteria criteria = SelectorCriteria.newBuilder()
                .withAsymmetricPart(AsymmetricPart.PUBLIC)
                .build();
        List<AtbashKey> atbashKeys = runtimeKeyManager.retrieveKeys(criteria);
        Assertions.assertThat(atbashKeys).hasSize(1);

        // Another read
        runtimeKeyManager.retrieveKeys(criteria);  // The keys should not be reloaded

        Mockito.verify(keyReaderMock, Mockito.times(1)).readKeyResource(PUBLIC_KEY_PEM);
    }

    @Test
    void retrieveKeys_noForce() {
        JWTAuthContextInfo info = new JWTAuthContextInfo();
        List<String> locations = new ArrayList<>();
        locations.add(PUBLIC_KEY_PEM);
        info.setPublicKeyLocation(locations);

        info.setDecryptionKeyLocation(Collections.emptyList());
        info.setKeysRefreshInterval(5); // 5 seconds
        info.setForcedKeysRefreshInterval(2); // 2 seconds
        Mockito.when(contextInfoProviderMock.getContextInfo()).thenReturn(info);

        List<AtbashKey> keys = List.of(getPublicKey());
        Mockito.when(keyReaderMock.readKeyResource(PUBLIC_KEY_PEM)).thenReturn(keys);
        SelectorCriteria criteria1 = SelectorCriteria.newBuilder()
                .withAsymmetricPart(AsymmetricPart.PUBLIC)
                .build();
        List<AtbashKey> atbashKeys1 = runtimeKeyManager.retrieveKeys(criteria1);
        Assertions.assertThat(atbashKeys1).hasSize(1);

        // No Private found but we don't trigger a reload
        SelectorCriteria criteria2 = SelectorCriteria.newBuilder()
                .withAsymmetricPart(AsymmetricPart.PRIVATE)
                .build();
        List<AtbashKey> atbashKeys2 = runtimeKeyManager.retrieveKeys(criteria2);
        Assertions.assertThat(atbashKeys2).isEmpty();

        Mockito.verify(keyReaderMock, Mockito.times(1)).readKeyResource(PUBLIC_KEY_PEM);
    }

    @Test
    void retrieveKeys_withForce() throws InterruptedException {
        JWTAuthContextInfo info = new JWTAuthContextInfo();
        List<String> locations = new ArrayList<>();
        locations.add(PUBLIC_KEY_PEM);
        info.setPublicKeyLocation(locations);

        info.setDecryptionKeyLocation(Collections.emptyList());
        info.setKeysRefreshInterval(5); // 5 seconds
        info.setForcedKeysRefreshInterval(1); // 1 seconds
        Mockito.when(contextInfoProviderMock.getContextInfo()).thenReturn(info);

        List<AtbashKey> keys = List.of(getPublicKey());
        Mockito.when(keyReaderMock.readKeyResource(PUBLIC_KEY_PEM)).thenReturn(keys);
        SelectorCriteria criteria1 = SelectorCriteria.newBuilder()
                .withAsymmetricPart(AsymmetricPart.PUBLIC)
                .build();
        List<AtbashKey> atbashKeys1 = runtimeKeyManager.retrieveKeys(criteria1);
        Assertions.assertThat(atbashKeys1).hasSize(1);

        TimeUnit.SECONDS.sleep(2);  // Longer then force refresh, so we are allowed to force a refresh of the keys when no key found
        // No Private found but we don't trigger a reload
        SelectorCriteria criteria2 = SelectorCriteria.newBuilder()
                .withAsymmetricPart(AsymmetricPart.PRIVATE)
                .build();
        List<AtbashKey> atbashKeys2 = runtimeKeyManager.retrieveKeys(criteria2);
        Assertions.assertThat(atbashKeys2).isEmpty();

        Mockito.verify(keyReaderMock, Mockito.times(2)).readKeyResource(PUBLIC_KEY_PEM);
    }


    private AtbashKey getPublicKey() {
        RSAGenerationParameters generationParameters = new RSAGenerationParameters.RSAGenerationParametersBuilder()
                .withKeyId("kid")
                .build();
        KeyGenerator generator = new KeyGenerator();
        List<AtbashKey> keys = generator.generateKeys(generationParameters);
        return keys.get(0);  // 1 = Private, 0 = public.
    }
}