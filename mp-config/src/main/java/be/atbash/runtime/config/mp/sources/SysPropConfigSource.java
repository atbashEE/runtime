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
package be.atbash.runtime.config.mp.sources;


import be.atbash.runtime.config.mp.util.ConfigSourceUtil;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class SysPropConfigSource extends AbstractConfigSource {
    private static final int DEFAULT_ORDINAL = 400;

    public SysPropConfigSource() {
        super("SysPropConfigSource", ConfigSourceUtil.getOrdinalFromMap(getSystemProperties(), DEFAULT_ORDINAL));
    }

    @Override
    public Map<String, String> getProperties() {
        return getSystemProperties();
    }

    @Override
    public Set<String> getPropertyNames() {
        return getProperties().keySet();
    }

    @Override
    public String getValue(String key) {
        return System.getProperty(key);
    }

    private static Map<String, String> getSystemProperties() {
        return Collections.unmodifiableMap(ConfigSourceUtil.propertiesToMap(System.getProperties()));
    }
}
