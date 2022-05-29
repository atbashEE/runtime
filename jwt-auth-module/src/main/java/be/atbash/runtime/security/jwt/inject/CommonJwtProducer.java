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

import be.atbash.ee.security.octopus.nimbus.util.JSONObjectUtils;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.json.JsonValue;
import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.lang.annotation.Annotation;
import java.util.Optional;

/**
 * A class that tracks the current validated MP-JWT and associated JsonWebToken via a thread
 * local to provide a @RequestScoped JsonWebToken producer method.
 * <p>
 * It also provides utility methods for access the current JsonWebToken claim values.
 * <p>
 * Based on the SmallRye code.
 */
@RequestScoped
public class CommonJwtProducer {

    @Inject
    private JsonWebToken currentToken;

    /**
     * Return the indicated claim value as a JsonValue
     *
     * @param ip - injection point of the claim
     * @return a JsonValue wrapper
     */
    public JsonValue generalJsonValueProducer(InjectionPoint ip) {
        String name = getName(ip);
        Object value = getValue(name);
        return JSONObjectUtils.getAsJsonValue(value);
    }

    public <T> T getValue(String name) {
        if (currentToken == null) {
            return null;
        }

        Optional<T> claimValue = currentToken.claim(name);
        return claimValue.orElse(null);
    }

    public String getName(InjectionPoint ip) {
        String name = null;
        for (Annotation ann : ip.getQualifiers()) {
            if (ann instanceof Claim) {
                Claim claim = (Claim) ann;
                name = claim.standard() == Claims.UNKNOWN ? claim.value() : claim.standard().name();
            }
        }
        // Should never be null (always a Claim on the InjectionPoint) but all code using this
        // method is valid when null is returned.
        return name;
    }

}
