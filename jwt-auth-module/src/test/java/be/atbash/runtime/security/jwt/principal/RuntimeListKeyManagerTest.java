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
import be.atbash.ee.security.octopus.keys.selector.AsymmetricPart;
import be.atbash.ee.security.octopus.keys.selector.SelectorCriteria;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeListKeyManagerTest {

    @Test
    void retrieveKeys() {

        RuntimeListKeyManager keyManager = new RuntimeListKeyManager(List.of(getPublicKey()));

        SelectorCriteria criteria = SelectorCriteria.newBuilder()
                .withId("kid")
                .withAsymmetricPart(AsymmetricPart.PUBLIC)
                .build();

        List<AtbashKey> atbashKeys = keyManager.retrieveKeys(criteria);
        Assertions.assertThat(atbashKeys).hasSize(1);
    }

    @Test
    void retrieveKeys_basedOnType() {

        RuntimeListKeyManager keyManager = new RuntimeListKeyManager(List.of(getPublicKey()));

        SelectorCriteria criteria = SelectorCriteria.newBuilder()
                .withId("SomethingElse")
                .withAsymmetricPart(AsymmetricPart.PUBLIC)
                .build();

        List<AtbashKey> atbashKeys = keyManager.retrieveKeys(criteria);
        Assertions.assertThat(atbashKeys).hasSize(1);
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