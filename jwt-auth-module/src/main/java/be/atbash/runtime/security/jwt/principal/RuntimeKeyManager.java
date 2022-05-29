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

import be.atbash.ee.security.octopus.keys.AbstractKeyManager;
import be.atbash.ee.security.octopus.keys.AtbashKey;
import be.atbash.ee.security.octopus.keys.KeyManager;
import be.atbash.ee.security.octopus.keys.ListKeyManager;
import be.atbash.ee.security.octopus.keys.reader.KeyReader;
import be.atbash.ee.security.octopus.keys.reader.UnknownKeyResourceTypeException;
import be.atbash.ee.security.octopus.keys.selector.SelectorCriteria;
import be.atbash.runtime.security.jwt.JWTAuthContextInfoProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class RuntimeKeyManager extends AbstractKeyManager implements KeyManager {

    private ListKeyManager keyManager;

    private LocalDateTime keysLoaded;

    private LocalDateTime forcedRefresh;

    @Inject
    private KeyReader keyReader;

    @Inject
    private JWTAuthContextInfoProvider contextInfoProvider;

    @Override
    public List<AtbashKey> retrieveKeys(SelectorCriteria selectorCriteria) {

        loadKeysIfNeeded();

        List<AtbashKey> result = getAtbashKeys(selectorCriteria);

        if (result.isEmpty()) {
            // Maybe we should try to refresh the keys?
            if (forcedRefreshAllowed()) {
                loadKeys();
                forcedRefresh = LocalDateTime.now();
                result = getAtbashKeys(selectorCriteria);
            }
        }

        return result;
    }

    private boolean forcedRefreshAllowed() {
        // When no forcedRefresh done, is last refresh longer then forcedRefresh config value?
        if (forcedRefresh == null && isLongerThenForcedRefreshPeriod(keysLoaded)) {
            return true;
        }
        // When Forced refresh already performed, longer then the Forced Refresh Config Value
        return forcedRefresh != null && isLongerThenForcedRefreshPeriod(forcedRefresh);
    }

    private boolean isLongerThenForcedRefreshPeriod(LocalDateTime referenceDate) {
        return referenceDate.plusSeconds(contextInfoProvider.getContextInfo().getForcedKeysRefreshInterval())
                .isBefore(LocalDateTime.now());
    }

    private List<AtbashKey> getAtbashKeys(SelectorCriteria selectorCriteria) {
        List<AtbashKey> atbashKeys = keyManager.retrieveKeys(selectorCriteria);
        if (atbashKeys.isEmpty()) {
            // not by name
            SelectorCriteria withoutId = SelectorCriteria.newBuilder(selectorCriteria).withId(null).build();
            atbashKeys = keyManager.retrieveKeys(withoutId);
        }
        return atbashKeys;
    }

    private void loadKeysIfNeeded() {
        if (refreshNeeded()) {
            loadKeys();
            keysLoaded = LocalDateTime.now();
            forcedRefresh = null;
        }
    }

    private boolean refreshNeeded() {
        boolean result = keysLoaded == null;
        if (!result) {
            result = keysLoaded.plusSeconds(contextInfoProvider.getContextInfo().getKeysRefreshInterval())
                    .isBefore(LocalDateTime.now());
        }
        return result;
    }

    private void loadKeys() {

        List<AtbashKey> keys = new ArrayList<>();
        for (String location : contextInfoProvider.getContextInfo().getPublicKeyLocation()) {
            try {
                keys.addAll(keyReader.readKeyResource(location));
            } catch (UnknownKeyResourceTypeException e) {
                keys.addAll(keyReader.tryToReadKeyResource(location, null));
            }
        }

        for (String location : contextInfoProvider.getContextInfo().getDecryptionKeyLocation()) {
            List<AtbashKey> decryptionKeys = keyReader.tryToReadKeyResource(location, null);
            // Only add the private key as we have otherwise multiple public ones and TCK doesn't use key Ids and thus confusion which one.
            // And in general the public one is not needed as this location is for decryption, and thus by definition we need the private key.
            // FIXME We need a filter for all private ones if we have http or JWK which can contain multiple.
            keys.add(decryptionKeys.get(0));
        }

        keyManager = new ListKeyManager(keys);
    }
}
