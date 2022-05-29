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
import be.atbash.ee.security.octopus.keys.reader.KeyReader;
import be.atbash.ee.security.octopus.keys.selector.KeySelector;
import jakarta.enterprise.inject.Vetoed;

import java.util.List;

@Vetoed
public class InlineKeySelector extends KeySelector {

    private KeyManager keyManager;

    public InlineKeySelector(String content) {
        defineKeys(content);
    }

    private void defineKeys(String content) {
        KeyReader keyReader = new KeyReader();
        List<AtbashKey> atbashKeys = keyReader.tryToReadKeyContent(content, null);
        keyManager = new RuntimeListKeyManager(atbashKeys);
    }

    @Override
    protected KeyManager getKeyManager() {
        return keyManager;
    }
}
