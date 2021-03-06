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
package be.atbash.runtime.core.data.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfigHelper {

    private static final Logger LOGGER  = LoggerFactory.getLogger(ConfigHelper.class);
    public static final String HTTP = "http";

    private ConfigHelper() {
    }

    public static Endpoint getHttpEndpoint(Config config) {
        return config.getEndpoints().stream()
                .filter(e -> HTTP.equals(e.getName()))
                .findAny()
                .orElseGet(ConfigHelper::createDefaultHttpEndpoint);

    }

    private static Endpoint createDefaultHttpEndpoint() {
        LOGGER.atWarn().log("CONFIG-010");
        Endpoint result = new Endpoint();
        result.setName("http");
        result.setPort(8080);
        return result;
    }
}
