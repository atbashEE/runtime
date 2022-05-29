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
package be.atbash.runtime.security.jwt.inject;

import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.json.*;
import org.eclipse.microprofile.jwt.Claim;

import java.util.Optional;

/**
 * A producer for JsonValue injection types.
 * <p>
 * Based on the SmallRye code.
 */
public class JsonValueProducer {
    @Inject
    private CommonJwtProducer util;

    @Produces
    @Claim("")
    public JsonString getJsonString(InjectionPoint ip) {
        return getValue(ip);
    }

    @Produces
    @Claim("")
    public Optional<JsonString> getOptionalJsonString(InjectionPoint ip) {
        return getOptionalValue(ip);
    }

    @Produces
    @Claim("")
    public JsonNumber getJsonNumber(InjectionPoint ip) {
        return getValue(ip);
    }

    @Produces
    @Claim("")
    public Optional<JsonNumber> getOptionalJsonNumber(InjectionPoint ip) {
        return getOptionalValue(ip);
    }

    @Produces
    @Claim("")
    public JsonArray getJsonArray(InjectionPoint ip) {
        return getValue(ip);
    }

    @Produces
    @Claim("")
    public Optional<JsonArray> getOptionalJsonArray(InjectionPoint ip) {
        return getOptionalValue(ip);
    }

    @Produces
    @Claim("")
    public JsonObject getJsonObject(InjectionPoint ip) {
        return getValue(ip);
    }

    @Produces
    @Claim("")
    public Optional<JsonObject> getOptionalJsonObject(InjectionPoint ip) {
        return getOptionalValue(ip);
    }

    @SuppressWarnings("unchecked")
    public <T extends JsonValue> T getValue(InjectionPoint ip) {
        return (T) util.generalJsonValueProducer(ip);
    }

    @SuppressWarnings("unchecked")
    public <T extends JsonValue> Optional<T> getOptionalValue(InjectionPoint ip) {
        T jsonValue = (T) util.generalJsonValueProducer(ip);
        return Optional.ofNullable(jsonValue);
    }
}
