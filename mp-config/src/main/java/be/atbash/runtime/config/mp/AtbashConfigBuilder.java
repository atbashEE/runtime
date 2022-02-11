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
import be.atbash.runtime.config.mp.sources.ConfigSources;
import be.atbash.runtime.config.mp.util.AnnotationUtil;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Implementation of the MicroProfile ConfigBuilder.  All the building logic related to {@link ConfigSource}s is
 * located in {@link ConfigSources}
 * <p/>
 * Based on code by Jeff Mesnil (Red Hat)
 */
public class AtbashConfigBuilder implements ConfigBuilder {

    // sources are not sorted by their ordinals
    // These are the sources defined by calling withSources()
    private final List<ConfigSource> sources = new ArrayList<>();
    // These are the converters that are added by calling withConverters()
    private final Map<Type, ConverterWithPriority> converters = new HashMap<>();

    private ClassLoader classLoader;
    private boolean addDefaultSources = false;
    private boolean addDefaultInterceptors = false;
    private boolean addDiscoveredSources = false;
    private boolean addDiscoveredConverters = false;
    private boolean addDiscoveredInterceptors = false;


    @Override
    public AtbashConfigBuilder addDiscoveredSources() {
        addDiscoveredSources = true;
        return this;
    }

    @Override
    public AtbashConfigBuilder addDiscoveredConverters() {
        addDiscoveredConverters = true;
        return this;
    }

    public AtbashConfigBuilder addDiscoveredInterceptors() {
        addDiscoveredInterceptors = true;
        return this;
    }

    @Override
    public AtbashConfigBuilder addDefaultSources() {
        addDefaultSources = true;
        return this;
    }

    public AtbashConfigBuilder addDefaultInterceptors() {
        this.addDefaultInterceptors = true;
        return this;
    }

    @Override
    public AtbashConfigBuilder forClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    @Override
    public AtbashConfigBuilder withSources(ConfigSource... configSources) {
        Collections.addAll(sources, configSources);
        return this;
    }

    @Override
    public AtbashConfigBuilder withConverters(Converter<?>[] converters) {
        for (Converter<?> converter : converters) {
            Type type = Converters.getConverterType(converter.getClass());
            if (type == null) {
                String msg = String.format("MPCONFIG-112: Can not add converter %s that is not parameterized with a type", converter);
                throw new IllegalStateException(msg);
            }
            int priority = AnnotationUtil.getPriority(converter.getClass()).orElse(ConfigSource.DEFAULT_ORDINAL);
            // Assumed default priority 100 as default, spec 6.2
            addConverter(type, priority, converter, this.converters);
        }
        return this;
    }

    @Override
    public <T> AtbashConfigBuilder withConverter(Class<T> type, int priority, Converter<T> converter) {
        addConverter(type, priority, converter, converters);
        return this;
    }

    private void addConverter(Type type, Converter<?> converter, Map<Type, ConverterWithPriority> converters) {
        int priority = AnnotationUtil.getPriority(converter.getClass()).orElse(ConfigSource.DEFAULT_ORDINAL);
        // Assumed default priority 100 as default, spec 6.2
        addConverter(type, priority, converter, converters);
    }

    private void addConverter(Type type, int priority, Converter<?> converter,
                              Map<Type, ConverterWithPriority> converters) {
        // add the converter only if it has a higher priority than another converter for the same type
        ConverterWithPriority oldConverter = converters.get(type);
        if (oldConverter == null || priority > oldConverter.priority) {
            converters.put(type, new ConverterWithPriority(converter, priority));
        }
    }


    public List<ConfigSource> getSources() {
        return sources;
    }

    public Map<Type, ConverterWithPriority> getConverters() {
        return converters;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public boolean isAddDefaultSources() {
        return addDefaultSources;
    }

    public boolean isAddDefaultInterceptors() {
        return addDefaultInterceptors;
    }

    public boolean isAddDiscoveredSources() {
        return addDiscoveredSources;
    }

    public boolean isAddDiscoveredInterceptors() {
        return addDiscoveredInterceptors;
    }

    private Map<Type, Converter<?>> buildConverters() {
        // Converters added through the builder
        Map<Type, AtbashConfigBuilder.ConverterWithPriority> convertersToBuild = new HashMap<>(getConverters());

        if (addDiscoveredConverters) {
            for (Converter<?> converter : discoverConverters()) {
                Type type = Converters.getConverterType(converter.getClass());
                if (type == null) {
                    String msg = String.format("MPCONFIG-112: Can not add converter %s that is not parameterized with a type", converter);
                    throw new IllegalStateException(msg);
                }
                addConverter(type, converter, convertersToBuild);
            }
        }

        ConcurrentHashMap<Type, Converter<?>> converters = new ConcurrentHashMap<>(Converters.ALL_CONVERTERS);
        for (Map.Entry<Type, AtbashConfigBuilder.ConverterWithPriority> entry : convertersToBuild.entrySet()) {
            converters.put(entry.getKey(), entry.getValue().getConverter());
        }

        return converters;
    }

    private List<Converter<?>> discoverConverters() {
        List<Converter<?>> discoveredConverters = new ArrayList<>();
        for (Converter<?> converter : ServiceLoader.load(Converter.class)) {
            discoveredConverters.add(converter);
        }
        return discoveredConverters;
    }


    @Override
    public AtbashConfig build() {
        Map<Type, Converter<?>> converters = buildConverters();
        return new AtbashConfig(this, converters);
    }

    static class ConverterWithPriority {
        private final Converter<?> converter;
        private final int priority;

        private ConverterWithPriority(Converter<?> converter, int priority) {
            this.converter = converter;
            this.priority = priority;
        }

        Converter<?> getConverter() {
            return converter;
        }
    }


}
