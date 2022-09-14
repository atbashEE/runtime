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
package be.atbash.runtime.config.mp.util;

import be.atbash.config.mp.ConfigValueImpl;
import be.atbash.config.mp.converter.Converters;
import be.atbash.config.mp.util.ConvertValueUtil;
import jakarta.enterprise.inject.spi.AnnotatedMember;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.InjectionPoint;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.Converter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Supplier;

/**
 * Actual implementations for producer method in CDI producer {@link be.atbash.runtime.config.mp.inject.ConfigProducer}.
 *
 * @author <a href="https://github.com/guhilling">Gunnar Hilling</a>
 */
public final class ConfigProducerUtil {

    private ConfigProducerUtil() {
    }

    /**
     * Retrieves a converted configuration value from {@link Config}.
     *
     * @param injectionPoint the {@link InjectionPoint} where the configuration value will be injected
     * @param config         the current {@link Config} instance.
     * @return the converted configuration value.
     */
    public static <T> T getValue(InjectionPoint injectionPoint, Config config) {
        String name = getName(injectionPoint);
        if (name == null) {
            return null;
        }
        return getValue(name, injectionPoint.getType(), getDefaultValue(injectionPoint), config);
    }

    /**
     * Retrieves a converted configuration value from {@link Config}.
     *
     * @param name         the name of the configuration property.
     * @param type         the {@link Type} of the configuration value to convert.
     * @param defaultValue the default value to use if no configuration value is found. (null if unconfigured)
     * @param config       the current {@link Config} instance.
     * @return the converted configuration value.
     */
    public static <T> T getValue(String name, Type type, String defaultValue, Config config) {
        if (name == null) {
            return null;
        }

        return ConvertValueUtil.convertValue(name, resolveDefault(getResolvedValue(name, config), defaultValue),
                resolveConverter(type, config));
    }

    public static ConfigValue getConfigValue(InjectionPoint injectionPoint, Config config) {
        String name = getName(injectionPoint);
        if (name == null) {
            return null;
        }

        ConfigValue configValue = config.getConfigValue(name);

        if (configValue.getRawValue() == null) {  // No value found for name within a ConfigSource.
            if (configValue instanceof ConfigValueImpl) {
                // Fall back to default value.
                return ((ConfigValueImpl) configValue).withValue(getDefaultValue(injectionPoint));
            }
        }

        return configValue;
    }

    private static String getResolvedValue(String name, Config config) {
        return config.getConfigValue(name).getValue();
    }

    private static String resolveDefault(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }

    private static <T> Converter<T> resolveConverter(Type type, Config config) {
        Class<T> rawType = rawTypeOf(type);
        if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (rawType == List.class) {
                return (Converter<T>) Converters.newCollectionConverter(resolveConverter(typeArgs[0], config), ArrayList::new);
            }
            if (rawType == Set.class) {
                return (Converter<T>) Converters.newCollectionConverter(resolveConverter(typeArgs[0], config), HashSet::new);
            }
            if (rawType == Optional.class) {
                return (Converter<T>) Converters.newOptionalConverter(resolveConverter(typeArgs[0], config));
            }
            if (rawType == Supplier.class) {
                return resolveConverter(typeArgs[0], config);
            }
        }
        // just try the raw type
        return config.getConverter(rawType).orElseThrow(() -> new IllegalArgumentException(String.format("MPCONFIG-207: No Converter registered for %s", rawType)));
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> rawTypeOf(Type type) {
        if (type instanceof Class<?>) {
            return (Class<T>) type;
        }
        if (type instanceof ParameterizedType) {
            return rawTypeOf(((ParameterizedType) type).getRawType());
        }
        if (type instanceof GenericArrayType) {
            return (Class<T>) Array.newInstance(rawTypeOf(((GenericArrayType) type).getGenericComponentType()), 0).getClass();
        }

        throw new IllegalArgumentException(String.format("MPCONFIG-038: Type has no raw type class: %s ", type));


    }

    private static String getName(InjectionPoint injectionPoint) {
        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier.annotationType().equals(ConfigProperty.class)) {
                ConfigProperty configProperty = ((ConfigProperty) qualifier);
                return getConfigKey(injectionPoint, configProperty, true);
            }
        }
        return null;
    }

    private static String getDefaultValue(InjectionPoint injectionPoint) {
        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier.annotationType().equals(ConfigProperty.class)) {
                String defaultString = ((ConfigProperty) qualifier).defaultValue();
                if (!ConfigProperty.UNCONFIGURED_VALUE.equals(defaultString)) {
                    return defaultString;
                }
                Class<?> rawType = rawTypeOf(injectionPoint.getType());
                return getDefaultForType(rawType);
            }
        }
        // But if no @ConfigProperty we can't determine key name so it is not a problem to return null here.
        return null;
    }

    public static String getDefaultForType(Class<?> rawType) {
        if (rawType.isPrimitive()) {
            if (rawType == char.class) {
                return null;
            }
            if (rawType == boolean.class) {
                return "false";
            }
            return "0";

        }
        return null;
    }

    /**
     * Determine the name for the configuration key based on the {@link  InjectionPoint} and the optional {@link ConfigProperty}.
     * It handles the correct rules regarding {@link org.eclipse.microprofile.config.inject.ConfigProperties}
     * simple names and fully qualified names for other scenarios.
     *
     * @param ip             The InjectionPoint
     * @param configProperty The optional configProperty.
     * @param fqn            true when the fully qualified name of the property is required (like package.class.propname) or false
     *                       when only property name is required (propname) as it is the case when using @ConfigProperties.
     * @return
     */
    public static String getConfigKey(InjectionPoint ip, ConfigProperty configProperty, boolean fqn) {
        if (configProperty != null) {
            String key = configProperty.name();
            if (!key.trim().isEmpty()) {
                return key;
            }
        }

        if (ip.getAnnotated() instanceof AnnotatedMember) {
            AnnotatedMember<?> member = (AnnotatedMember<?>) ip.getAnnotated();
            if (!fqn) {
                return member.getJavaMember().getName();
            }

            AnnotatedType<?> declaringType = member.getDeclaringType();
            if (declaringType != null) {
                return declaringType.getJavaClass().getCanonicalName() + '.' + member.getJavaMember().getName();
            }

        }
        throw new IllegalStateException(String.format("MPCONFIG-202: Could not determine default name for @ConfigProperty InjectionPoint '%s' ", ip));

    }

}
