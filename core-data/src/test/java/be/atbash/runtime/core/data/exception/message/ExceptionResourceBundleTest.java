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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;

class ExceptionResourceBundleTest {

    @Test
    void addModule() {
        ExceptionResourceBundle bundle = new ExceptionResourceBundle(Locale.ENGLISH);
        bundle.addModule("core-data");
        Object object = bundle.handleGetObject("MODULE-001");
        Assertions.assertThat(object).isEqualTo("MODULE-001: Abort");
    }

    @Test
    void handleGetObject_localeSpecific() {
        ExceptionResourceBundle bundle = new ExceptionResourceBundle(Locale.forLanguageTag("nl"));
        bundle.addModule("core-data");
        bundle.addModule("junit");
        Object object = bundle.handleGetObject("TEST01");
        Assertions.assertThat(object).isEqualTo("Test01 - Test Message for 'nl' language");
    }

    @Test
    void handleGetObject_localeSpecific_WhenLanguageNotAvailable() {
        ExceptionResourceBundle bundle = new ExceptionResourceBundle(Locale.forLanguageTag("nl"));
        bundle.addModule("core-data");
        bundle.addModule("junit");
        Object object = bundle.handleGetObject("MODULE-001");
        Assertions.assertThat(object).isEqualTo("MODULE-001: Abort");
    }

    @Test
    void handleGetObject_localeSpecific_LooksupGenericLanguage() {
        ExceptionResourceBundle bundle = new ExceptionResourceBundle(Locale.forLanguageTag("nl"));
        bundle.addModule("core-data");
        bundle.addModule("junit");
        Object object = bundle.handleGetObject("TEST02");
        Assertions.assertThat(object).isEqualTo("Test02 - Only in main language : parameter = {0}");
    }

    @Test
    void getKeys() {
        ExceptionResourceBundle bundle = new ExceptionResourceBundle(Locale.forLanguageTag("nl"));
        bundle.addModule("core-data");
        bundle.addModule("junit");
        Enumeration<String> keys = bundle.getKeys();
        // core-data is a live file and thus we don't care if additional keys show up. We at least need to test if
        // keys from each resourcebundle show up.
        Assertions.assertThat(Collections.list(keys)).contains("MODULE-001", "TEST01");
    }

}