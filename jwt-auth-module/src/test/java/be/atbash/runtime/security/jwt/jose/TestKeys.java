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
package be.atbash.runtime.security.jwt.jose;

import be.atbash.ee.security.octopus.keys.AtbashKey;
import be.atbash.ee.security.octopus.keys.generator.*;
import be.atbash.ee.security.octopus.keys.selector.SelectorCriteria;
import be.atbash.ee.security.octopus.keys.selector.filter.KeyFilter;

import java.util.List;

public final class TestKeys {

    private TestKeys() {
    }

    public static List<AtbashKey> generateRSAKeys(String kid, SelectorCriteria selectorCriteria) {
        return generateRSAKeys(kid, 2048, selectorCriteria);
    }

    public static List<AtbashKey> generateRSAKeys(String kid, int keySize, SelectorCriteria selectorCriteria) {
        RSAGenerationParameters generationParameters = new RSAGenerationParameters.RSAGenerationParametersBuilder()
                .withKeyId(kid)
                .withKeySize(keySize)
                .build();
        KeyGenerator generator = new KeyGenerator();
        List<AtbashKey> result = generator.generateKeys(generationParameters);

        if (selectorCriteria != null) {
            List<KeyFilter> filters = selectorCriteria.asKeyFilters();
            for (KeyFilter filter : filters) {
                result = filter.filter(result);
            }
        }
        return result;
    }
}
