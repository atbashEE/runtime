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
package be.atbash.runtime.core.data.exception.message;

import be.atbash.runtime.core.data.exception.UnexpectedException;
import be.atbash.runtime.core.data.util.ResourceReader;
import be.atbash.util.resource.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

class ExceptionResourceBundle extends ResourceBundle {

    private final Locale locale;
    private final Map<String, ResourceBundle> bundlesLocaleSpecific;
    private final Map<String, ResourceBundle> bundles;

    public ExceptionResourceBundle(Locale locale) {
        this.locale = locale;
        bundlesLocaleSpecific = new HashMap<>();
        bundles = new HashMap<>();
    }

    @Override
    protected Object handleGetObject(String key) {
        return bundlesLocaleSpecific.values().stream()
                .filter(rb -> rb.containsKey(key))
                .findAny()
                .map(rb -> rb.getObject(key))
                .orElse(handleGetObjectWithGenericLanguage(key));

    }

    private Object handleGetObjectWithGenericLanguage(String key) {
        return bundles.values().stream()
                .filter(rb -> rb.containsKey(key))
                .findAny()
                .map(rb -> rb.getObject(key))
                .orElse(key);
    }

    @Override
    public Enumeration<String> getKeys() {
        Set<String> keys = bundlesLocaleSpecific.values().stream()
                .map(ResourceBundle::keySet)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        keys.addAll(bundles.values().stream()
                .map(ResourceBundle::keySet)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet()));
        return Collections.enumeration(keys);
    }

    public void addModule(String moduleName) {
        String path = definePath(moduleName, true);
        addResourceBundle(moduleName, path, bundlesLocaleSpecific);

        path = definePath(moduleName, false);
        addResourceBundle(moduleName, path, bundles);
    }

    private void addResourceBundle(String moduleName, String path, Map<String, ResourceBundle> bundles) {
        if (ResourceReader.existsResource(path)) {
            PropertyResourceBundle bundle;
            try {
                InputStream inputStream = ResourceReader.getStream(path);
                bundle = new PropertyResourceBundle(inputStream);
                inputStream.close();
            } catch (IOException e) {
                throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
            }
            bundles.put(moduleName, bundle);
        }
    }

    private String definePath(String moduleName, boolean withLocale) {
        StringBuilder result = new StringBuilder();
        result.append(ResourceUtil.CLASSPATH_PREFIX).append("msg/exception/").append(moduleName);
        if (withLocale) {
            result.append("_");
            result.append(locale.toString());
        }
        result.append(".properties");
        return result.toString();
    }
}
