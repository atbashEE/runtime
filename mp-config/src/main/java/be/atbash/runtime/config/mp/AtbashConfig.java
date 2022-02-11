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
package be.atbash.runtime.config.mp;

import be.atbash.runtime.config.mp.converter.Converters;
import be.atbash.runtime.config.mp.converter.ImplicitConverters;
import be.atbash.runtime.config.mp.sources.ConfigSources;
import be.atbash.runtime.config.mp.util.ConvertValueUtil;
import be.atbash.util.CollectionUtils;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@code Config} implementation
 * <p>
 * Based on code by Jeff Mesnil (Red Hat)
 */
public class AtbashConfig implements Config, Serializable {
    public static final String CONFIG_PROFILE_KEY = "mp.config.profile";

    private final ConfigSources configSources;
    private final Map<Type, Converter<?>> converters;

    // Caching the converters for Optional<T>
    private final Map<Type, Converter<Optional<?>>> optionalConverters = new ConcurrentHashMap<>();

    AtbashConfig(AtbashConfigBuilder builder, Map<Type, Converter<?>> converters) {
        this.configSources = new ConfigSources(builder);
        this.converters = converters;
    }

    @Override
    public <T> T getValue(String name, Class<T> aClass) {
        if (aClass.equals(ConfigValue.class)) {
            ConfigValue configValue = configSources.getInterceptorChain().proceed(name);
            if (configValue == null) {
                String msg = String.format("MPCONFIG-114: The config property '%s' is required but it could not be found in any config source", name);
                throw new NoSuchElementException(msg);
            }
            return (T) configValue;
        }
        return getValue(name, requireConverter(aClass));
    }

    /**
     * This method handles calls from both {@link Config#getValue} and {@link Config#getOptionalValue}.
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(String name, Converter<T> converter) {
        ConfigValue configValue = getConfigValue(name);

        String value = configValue.getValue(); // Can return the empty String (which is not considered as null)

        return ConvertValueUtil.convertValue(name, value, converter);
    }

    public ConfigValue getConfigValue(String name) {
        ConfigValue configValue = configSources.getInterceptorChain().proceed(name);
        return configValue != null ? configValue : ConfigValueImpl.builder().withName(name).build();
    }

    @Override
    public <T> Optional<T> getOptionalValue(String name, Class<T> aClass) {
        if (aClass.equals(ConfigValue.class)) {
            ConfigValue configValue = configSources.getInterceptorChain().proceed(name);
            return (Optional<T>) Optional.of(configValue);
        }
        return (Optional<T>) getValue(name, getOptionalConverter(aClass));
    }

    @Override
    public Iterable<String> getPropertyNames() {
        Iterator<String> namesIterator = configSources.getInterceptorChain().iterateNames();
        return CollectionUtils.iteratorToIterable(namesIterator);

    }

    @Override
    public Iterable<ConfigSource> getConfigSources() {
        return configSources.getSources();
    }

    public <T> T convert(String value, Class<T> asType) {
        return value != null ? requireConverter(asType).convert(value) : null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> Converter<Optional<?>> getOptionalConverter(Class<T> asType) {
        return optionalConverters.computeIfAbsent(asType, clazz -> Converters.newOptionalConverter(requireConverter((Class) clazz)));
    }

    @Override
    public <T> Optional<Converter<T>> getConverter(Class<T> asType) {
        Optional<Converter<?>> result = findConverter(asType);
        // Short hack to work around Type definition
        return result.map(c -> (Converter<T>) c);
    }

    public List<Converter<?>> getConverters() {
        return new ArrayList<>(converters.values());
    }

    @SuppressWarnings("unchecked")
    private <T> Converter<T> requireConverter(Class<T> asType) {
        Optional<Converter<?>> converter = findConverter(asType);
        if (converter.isEmpty()) {
            throw new IllegalArgumentException("No Converter registered for %s " + asType);
        }
        return (Converter<T>) converter.get();
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        if (Config.class.isAssignableFrom(type)) {
            return type.cast(this);
        }
        String msg = String.format("MPCONFIG-036: Type %s not supported for unwrapping.", type);
        throw new IllegalArgumentException(msg);
    }

    @SuppressWarnings("unchecked")
    private Optional<Converter<?>> findConverter(Class<?> asType) {
        Converter<?> exactConverter = converters.get(asType);
        if (exactConverter != null) {
            return Optional.of(exactConverter);
        }
        if (asType.isPrimitive()) {
            return findConverter(Converters.wrapPrimitiveType(asType));
        }
        if (asType.isArray()) {
            Optional<Converter<?>> arrayTypeConverter = findConverter(asType.getComponentType());
            return arrayTypeConverter.map(c -> Converters.newArrayConverter(c, asType));
        }
        return Optional.ofNullable(converters.computeIfAbsent(asType, clazz -> ImplicitConverters.getConverter((Class<?>) clazz)));
    }


    private Object writeReplace() throws ObjectStreamException {
        return RegisteredConfig.instance;
    }

    /**
     * Serialization placeholder which deserializes to the current registered config
     */
    private static class RegisteredConfig implements Serializable {
        private static final long serialVersionUID = 1L;
        private static final RegisteredConfig instance = new RegisteredConfig();

        private Object readResolve() throws ObjectStreamException {
            return ConfigProvider.getConfig();
        }
    }


}
