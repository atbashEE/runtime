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

import java.io.IOException;
import java.net.URL;

/**
 * A properties ConfigSource loaded from a URL (can be a file)
 *
 * Based on code by Jeff Mesnil (c) 2017 Red Hat inc.
 */
public class PropertiesConfigSource extends MapBackedConfigSource {

    private static final String NAME_PREFIX = "PropertiesConfigSource[source=";

    /**
     * Construct a new instance
     *
     * @param url a property file location
     * @throws IOException if an error occurred when reading from the input stream
     */

    public PropertiesConfigSource(URL url, int ordinal) throws IOException {
        super(NAME_PREFIX + url.toString() + "]", ConfigSourceUtil.urlToMap(url), ordinal);
    }

}
