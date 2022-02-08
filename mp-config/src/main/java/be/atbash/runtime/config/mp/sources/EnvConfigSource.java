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


import be.atbash.runtime.config.mp.util.StringUtil;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;


/**
 *
 *
 * Based on code by Jeff Mesnil (c) 2017 Red Hat inc.
 */
public class EnvConfigSource extends MapBackedConfigSource {

    private static final int DEFAULT_ORDINAL = 300;

    public EnvConfigSource() {
        this(DEFAULT_ORDINAL);
    }

    public EnvConfigSource(int ordinal) {
        this(getEnvProperties(), ordinal);
    }

    public EnvConfigSource(Map<String, String> propertyMap, int ordinal) {
        super("EnvConfigSource", propertyMap, ordinal);
    }

    @Override
    public String getValue(String propertyName) {
        return getValue(propertyName, getProperties());
    }

    private static String getValue(String name, Map<String, String> properties) {
        if (name == null) {
            return null;
        }

        // exact match
        String value = properties.get(name);
        if (value != null) {
            return value;
        }

        // replace non-alphanumeric characters by underscores
        String sanitizedName = StringUtil.replaceNonAlphanumericByUnderscores(name);
        value = properties.get(sanitizedName);
        if (value != null) {
            return value;
        }

        // replace non-alphanumeric characters by underscores and convert to uppercase
        return properties.get(sanitizedName.toUpperCase());
    }

    /**
     * A new Map with the contents of System.getEnv.
     */
    private static Map<String, String> getEnvProperties() {
        return Collections.unmodifiableMap(System.getenv());
    }

    Object writeReplace() {
        return new Ser();
    }

    static final class Ser implements Serializable {
        private static final long serialVersionUID = 6812312718645271331L;

        Object readResolve() {
            return new EnvConfigSource();
        }
    }
}
