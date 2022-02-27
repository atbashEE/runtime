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
package be.atbash.runtime.logging.mapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class BundleMapping {

    private static final BundleMapping INSTANCE = new BundleMapping();

    private final Map<String, String> mappings = new HashMap<>();

    private BundleMapping() {
    }

    public void addMapping(String oldResourceBundleName, String newResourceBundleName) {
        mappings.put(oldResourceBundleName, newResourceBundleName);
        // TODO Define an interface and SPI that at deployment we give user the opportunity to specify the mapping for their own application.
    }

    public String defineBundleName(String name) {
        String finalName = Optional.ofNullable(mappings.get(name)).orElse(name);
        return "msg." + finalName;
    }

    public static BundleMapping getInstance() {
        return INSTANCE;
    }


}
