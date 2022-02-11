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
package be.atbash.runtime.config.mp.inject;

import be.atbash.runtime.config.mp.AtbashConfig;
import be.atbash.runtime.config.mp.util.ConfigProducerUtil;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.*;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Create a CDI bean for the specified Class that can be injected for ConfigProperty.
 * <p>
 * Based on code by Mark Struberg
 */
public class ConfigInjectionBean<T> implements Bean<T>, PassivationCapable {

    private static final Set<Annotation> QUALIFIERS = new HashSet<>();

    static {
        QUALIFIERS.add(new ConfigPropertyLiteral("", ""));
    }

    private final BeanManager bm;
    private final Class<?> clazz;

    /**
     * only access via {@link #getConfig()}
     */
    private Config config;

    public ConfigInjectionBean(BeanManager bm, Class<?> clazz) {
        this.bm = bm;
        this.clazz = clazz;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public Class<?> getBeanClass() {
        return ConfigInjectionBean.class;
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T create(CreationalContext<T> context) {
        InjectionPoint ip = (InjectionPoint) bm.getInjectableReference(new MetadataInjectionPoint(), context);
        Annotated annotated = ip.getAnnotated();
        ConfigProperty configProperty = annotated.getAnnotation(ConfigProperty.class);
        String key = ConfigProducerUtil.getConfigKey(ip, configProperty, true);
        String defaultValue = configProperty.defaultValue();

        if (annotated.getBaseType() instanceof Class) {
            Class<?> annotatedTypeClass = (Class<?>) annotated.getBaseType();
            if (defaultValue.length() == 0) {
                return (T) getConfig().getValue(key, annotatedTypeClass);
            } else {
                Optional<T> optionalValue = (Optional<T>) getConfig().getOptionalValue(key, annotatedTypeClass);
                return optionalValue.orElseGet(
                        () -> (T) ((AtbashConfig) getConfig()).convert(defaultValue, annotatedTypeClass));
            }
        }

        throw new IllegalStateException("MPCONFIG-203: Unhandled ConfigProperty");
    }

    public Config getConfig() {
        if (config == null) {
            config = ConfigProvider.getConfig();
        }
        return config;
    }

    @Override
    public void destroy(T instance, CreationalContext<T> context) {

    }

    @Override
    public Set<Type> getTypes() {
        return Collections.singleton(clazz);
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return QUALIFIERS;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return Dependent.class;
    }

    @Override
    public String getName() {
        return "ConfigInjectionBean_" + clazz;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public String getId() {
        return "ConfigInjectionBean_" + clazz;
    }
}
