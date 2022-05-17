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
package be.atbash.runtime.core.data.util;

import java.util.HashMap;
import java.util.Map;

public final class SystemPropertyUtil {

    private static final SystemPropertyUtil INSTANCE = new SystemPropertyUtil();
    private static final String DEFAULT = "Default";

    private final Map<String, Boolean> resultCache = new HashMap<>();

    private SystemPropertyUtil() {
    }

    public boolean isTck(String module) {
        return resultCache.computeIfAbsent("tck", key -> {

            String property = System.getProperty("atbash.runtime.tck." + module, DEFAULT);
            if (!DEFAULT.equals(property)) {
                return Boolean.parseBoolean(property);
            }

            return Boolean.parseBoolean(System.getProperty("atbash.runtime.tck", "false"));
        });
    }

    public static SystemPropertyUtil getInstance() {
        return INSTANCE;
    }

}
