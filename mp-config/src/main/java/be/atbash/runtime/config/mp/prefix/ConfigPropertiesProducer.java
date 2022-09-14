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
package be.atbash.runtime.config.mp.prefix;

import be.atbash.config.mp.AtbashConfig;
import be.atbash.runtime.config.mp.util.AnnotationUtil;
import be.atbash.runtime.config.mp.util.ConfigProducerUtil;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.*;
import jakarta.inject.Provider;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Based on code from Payara MicroProfile Config implementation.
 */
public class ConfigPropertiesProducer {

    private static final Logger LOGGER = Logger.getLogger(ConfigPropertiesProducer.class.getName());
    private static Config config;

    // Don't change the name of this method unless also changed in be.atbash.runtime.config.mp.ConfigExtension.registerConfigPropertiesBean
    @ConfigProperties
    public static Object getGenericObject(InjectionPoint injectionPoint, BeanManager bm)
            throws InstantiationException, IllegalAccessException {
        Type type = injectionPoint.getType();
        if (!(type instanceof Class)) {
            throw new IllegalArgumentException("Unable to process injection point with @ConfigProperties of type " + type);
        }

        // Initialise the object. This may throw exceptions

        Object object;
        try {
            object = ((Class) type).getDeclaredConstructor().newInstance();
        } catch (InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalArgumentException("Unable to create instance of type " + type);
        }


        // Model the class
        AnnotatedType<?> annotatedType = bm.createAnnotatedType((Class) type);

        // Find the @ConfigProperties annotations, and calculate the property prefix
        ConfigProperties injectionAnnotation = getQualifier(injectionPoint);
        ConfigProperties classAnnotation = annotatedType.getAnnotation(ConfigProperties.class);
        String prefix = parsePrefixes(injectionAnnotation, classAnnotation);

        for (AnnotatedField<?> field : annotatedType.getFields()) {

            // Find the java field and field name
            Field javaField = field.getJavaMember();

            // Make sure the field is accessible
            javaField.setAccessible(true);

            InjectionPoint fieldInjectionPoint = bm.createInjectionPoint(field);

            try {
                Object value = getValue(fieldInjectionPoint, prefix);

                if (value != null) {
                    javaField.set(object, value);
                }
            } catch (Exception ex) {
                if (javaField.get(object) == null) {

                    LOGGER.log(Level.WARNING, String.format("Unable to inject property with name %s into type %s.",
                            javaField.getName(), javaField.getType().getName()), ex);


                    throw ex;
                }
            }
        }

        return object;
    }

    private static <T> T getValue(InjectionPoint fieldInjectionPoint, String prefix) {
        Annotated annotated = fieldInjectionPoint.getAnnotated();
        ConfigProperty configProperty = annotated.getAnnotation(ConfigProperty.class);
        String key = prefix + ConfigProducerUtil.getConfigKey(fieldInjectionPoint, configProperty, false);
        String defaultValue = defineDefaultValue(configProperty);

        if (annotated.getBaseType() instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) annotated.getBaseType();
            Type rawType = paramType.getRawType();

            // handle Provider<T> and Instance<T>
            if (rawType instanceof Class
                    && (((Class<?>) rawType).isAssignableFrom(Provider.class)
                    || ((Class<?>) rawType).isAssignableFrom(Instance.class))
                    && paramType.getActualTypeArguments().length == 1) {
                Class<?> paramTypeClass = (Class<?>) paramType.getActualTypeArguments()[0];
                return (T) getConfig().getValue(key, paramTypeClass);
            }
            Class<?> paramTypeClass = (Class<?>) paramType.getActualTypeArguments()[0];
            return (T) getConfig().getOptionalValue(key, paramTypeClass);
        } else {
            Class<?> annotatedTypeClass = (Class<?>) annotated.getBaseType();
            if (defaultValue.length() == 0) {
                return (T) getConfig().getValue(key, annotatedTypeClass);
            } else {
                Optional<T> optionalValue = (Optional<T>) getConfig().getOptionalValue(key, annotatedTypeClass);
                return optionalValue.orElseGet(
                        () -> (T) ((AtbashConfig) getConfig()).convert(defaultValue, annotatedTypeClass));
            }
        }

    }

    private static String defineDefaultValue(ConfigProperty configProperty) {
        String defaultValue = "";
        if (configProperty != null) {
            defaultValue = configProperty.defaultValue();
        }
        return defaultValue;
    }

    private static Config getConfig() {
        if (config == null) {
            config = ConfigProvider.getConfig();
        }
        return config;
    }

    private static ConfigProperties getQualifier(InjectionPoint injectionPoint) {

        // If it's an @Inject point
        ConfigProperties result = AnnotationUtil.getConfigPropertiesAnnotation(injectionPoint);

        if (result == null) {
            // If it's a programmatic lookup
            Set<Annotation> qualifiers = injectionPoint.getQualifiers();
            for (Annotation qualifier : qualifiers) {
                if (qualifier instanceof ConfigProperties) {
                    return (ConfigProperties) qualifier;
                }
            }
        }
        return result;
    }


    private static String parsePrefixes(ConfigProperties injectionAnnotation, ConfigProperties classAnnotation) {
        Optional<String> result = AnnotationUtil.parsePrefix(injectionAnnotation);
        if (result.isPresent()) {
            return result.get();
        }
        result = AnnotationUtil.parsePrefix(classAnnotation);
        return result.orElse("");
    }


}
