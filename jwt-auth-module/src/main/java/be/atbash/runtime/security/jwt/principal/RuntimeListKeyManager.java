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
import be.atbash.ee.security.octopus.keys.KeyManager;
import be.atbash.ee.security.octopus.keys.ListKeyManager;
import be.atbash.ee.security.octopus.keys.selector.SelectorCriteria;
import be.atbash.util.exception.AtbashIllegalActionException;
import jakarta.enterprise.inject.Vetoed;

import java.util.List;

@Vetoed
public class RuntimeListKeyManager implements KeyManager {

    private final List<AtbashKey> keys;

    public RuntimeListKeyManager(List<AtbashKey> keys) {
        this.keys = keys;
    }

    @Override
    public List<AtbashKey> retrieveKeys(SelectorCriteria selectorCriteria) {
        if (selectorCriteria == null) {
            throw new AtbashIllegalActionException("Parameter selectorCriteria can't be null");
        }

        ListKeyManager keyManager = new ListKeyManager(keys);

        List<AtbashKey> atbashKeys = keyManager.retrieveKeys(selectorCriteria);
        if (atbashKeys.isEmpty()) {
            // not by name
            SelectorCriteria withoutId = SelectorCriteria.newBuilder(selectorCriteria).withId(null).build();
            atbashKeys = keyManager.retrieveKeys(withoutId);
        }
        return atbashKeys;
    }
}
