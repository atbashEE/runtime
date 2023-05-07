/*
 * Copyright 2021-2023 Rudy De Busscher (https://www.atbash.be)
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

import be.atbash.ee.security.octopus.nimbus.util.JSONObjectUtils;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.json.JsonNumber;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.lang.annotation.Annotation;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Based on the SmallRye code.
 */
@Dependent
public class RawClaimTypeProducer {
    @Inject
    private JsonWebToken currentToken;

    @Produces
    @Claim("")
    Set<String> getClaimAsSet(InjectionPoint ip) {
        if (currentToken == null) {
            return null;
        }

        String name = getClaimName(ip);
        Optional<Object> optValue = currentToken.claim(name);
        return optValue.map(val -> new HashSet<>(JSONObjectUtils.getAsList(val))).orElse(null);
    }

    @Produces
    @Claim("")
    String getClaimAsString(InjectionPoint ip) {
        if (currentToken == null) {
            return null;
        }

        String name = getClaimName(ip);
        Optional<Object> optValue = currentToken.claim(name);
        String returnValue = null;
        if (optValue.isPresent()) {
            Object value = optValue.get();
            if (value instanceof JsonString) {
                JsonString jsonValue = (JsonString) value;
                returnValue = jsonValue.getString();
            } else {
                returnValue = value.toString();
            }
        }
        return returnValue;
    }

    @Produces
    @Claim("")
    Long getClaimAsLong(InjectionPoint ip) {
        if (currentToken == null) {
            return null;
        }

        String name = getClaimName(ip);
        Optional<Object> optValue = currentToken.claim(name);
        Long returnValue = null;
        if (optValue.isPresent()) {
            Object value = optValue.get();
            if (value instanceof Long) {
                returnValue = (Long) value;
            } else if (value instanceof JsonNumber) {
                JsonNumber jsonValue = (JsonNumber) value;
                returnValue = jsonValue.longValue();
            } else {
                returnValue = Long.parseLong(value.toString());
            }
        }
        return returnValue;
    }

    @Produces
    @Claim("")
    Date getClaimAsDate(InjectionPoint ip) {
        if (currentToken == null) {
            return null;
        }

        String name = getClaimName(ip);
        Optional<Object> optValue = currentToken.claim(name);
        Date returnValue = null;
        if (optValue.isPresent()) {
            Object value = optValue.get();
            long time;
            if (value instanceof Long) {
                time = (long) value;
            } else if (value instanceof JsonNumber) {
                time = ((JsonNumber) value).longValue();
            } else {
                time = Long.parseLong(value.toString());
            }
            // this produces a date in GMT timezone!
            returnValue = new Date(time * 1000L);
        }
        return returnValue;
    }

    @Produces
    @Claim("")
    Double getClaimAsDouble(InjectionPoint ip) {
        if (currentToken == null) {
            return null;
        }

        String name = getClaimName(ip);
        Optional<Object> optValue = currentToken.claim(name);
        Double returnValue = null;
        if (optValue.isPresent()) {
            Object value = optValue.get();
            if (value instanceof JsonNumber) {
                JsonNumber jsonValue = (JsonNumber) value;
                returnValue = jsonValue.doubleValue();
            } else {
                returnValue = Double.parseDouble(value.toString());
            }
        }
        return returnValue;
    }

    @Produces
    @Claim("")
    Boolean getClaimAsBoolean(InjectionPoint ip) {
        if (currentToken == null) {
            return null;
        }

        String name = getClaimName(ip);
        Optional<Object> optValue = currentToken.claim(name);
        Boolean returnValue = null;
        if (optValue.isPresent()) {
            Object value = optValue.get();
            if (value instanceof JsonValue) {
                final JsonValue.ValueType valueType = ((JsonValue) value).getValueType();
                if (valueType.equals(JsonValue.ValueType.TRUE)) {
                    returnValue = true;
                } else if (valueType.equals(JsonValue.ValueType.FALSE)) {
                    returnValue = false;
                }
            } else {
                returnValue = Boolean.valueOf(value.toString());
            }
        }
        return returnValue;
    }

    /**
     * Produces a *raw* Optional value.
     *
     * @param ip reference to the injection point
     * @return an optional claim value
     */
    @Produces
    @Claim
    @Dependent
    public <T> Optional<T> getOptionalValue(InjectionPoint ip) {
        if (currentToken == null) {
            return Optional.empty();
        }
        return currentToken.claim(getClaimName(ip));
    }

    static String getClaimName(InjectionPoint ip) {
        String name = null;
        for (Annotation ann : ip.getQualifiers()) {
            if (ann instanceof Claim) {
                Claim claim = (Claim) ann;
                name = claim.standard() == Claims.UNKNOWN ? claim.value() : claim.standard().name();
            }
        }
        return name;
    }
}
