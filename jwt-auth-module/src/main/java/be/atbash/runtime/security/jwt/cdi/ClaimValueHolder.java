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
package be.atbash.runtime.security.jwt.cdi;

import be.atbash.ee.security.octopus.nimbus.util.JSONObjectUtils;
import be.atbash.runtime.security.jwt.inject.CommonJwtProducer;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.json.JsonValue;
import org.eclipse.microprofile.jwt.ClaimValue;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.Provider;
import java.util.Optional;
import java.util.Set;

/**
 * An implementation of the ClaimValue interface
 * <p>
 * <p>
 * Based on SmallRye JWT
 *
 * @param <T> the claim value type
 */
public class ClaimValueHolder<T> implements ClaimValue<T> {
    private final CommonJwtProducer producer;
    private final String name;
    private final boolean optional;
    private final Class<?> claimValueClass;

    public ClaimValueHolder(InjectionPoint ip, CommonJwtProducer producer) {
        this.producer = producer;
        name = producer.getName(ip);
        Type injectedType = ip.getType();

        if (injectedType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) injectedType;
            Type typeArgument = parameterizedType.getActualTypeArguments()[0];
            // Check if the injection point is optional, i.e. ClaimValue<<Optional<?>>
            optional = typeArgument.getTypeName().startsWith(Optional.class.getTypeName());
        } else {
            optional = false;
        }

        claimValueClass = unwrapType(ip.getType());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getValue() {
        Object value = producer.getValue(name);

        if (!(value instanceof JsonValue) && JsonValue.class.isAssignableFrom(claimValueClass)) {
            // Klass indicates it must be a JsonValue but producer created a Java Object -> convert it.
            value = JSONObjectUtils.getAsJsonValue(value);
        }

        if (optional) {
            /*
             * Wrap the raw value in Optional based on type parameter of the
             * ClaimValue checked during construction.
             */
            return (T) Optional.ofNullable(value);
        }

        return (T) value;
    }

    @Override
    public String toString() {
        T value = getValue();
        return String.format("ClaimValueWrapper[@%s], name=%s, value[%s]=%s",
                Integer.toHexString(hashCode()),
                name, value.getClass(), value);
    }

    private Class<?> unwrapType(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            if (parameterizedType.getActualTypeArguments().length == 1) {
                Type rawType = parameterizedType.getRawType();
                Type actualType = parameterizedType.getActualTypeArguments()[0];

                if (rawType == ClaimValue.class) {
                    return unwrapType(actualType);
                }

                if (rawType == Optional.class) {
                    // Needs to be improved, so we don't have a separate boolean flag if Optional. Kept this way to minimize code changes.
                    return unwrapType(actualType);
                }

                if (rawType instanceof Class && Set.class.isAssignableFrom((Class<?>) rawType)) {
                    return (Class<?>) rawType;
                }

                if (rawType == Provider.class || rawType == Instance.class) {
                    return unwrapType(actualType);
                }
            }
        } else if (type instanceof Class) {
            Class<?> klass = (Class<?>) type;
            if (Long.class.isAssignableFrom(klass) || klass == long.class ||
                    Boolean.class.isAssignableFrom(klass) || klass == boolean.class ||
                    String.class.isAssignableFrom(klass) ||
                    JsonValue.class.isAssignableFrom(klass) ||
                    ClaimValue.class.isAssignableFrom(klass) ||
                    Optional.class.isAssignableFrom(klass)) {

                return klass;
            }
        }

        // We should throw DeploymentException here, but we never had validation on supported injection types, do it is
        // possible to inject non supported types as long as you get the right type from the claim set.
        return null;
    }
}
