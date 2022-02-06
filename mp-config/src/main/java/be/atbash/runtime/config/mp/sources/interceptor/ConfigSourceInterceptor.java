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
package be.atbash.runtime.config.mp.sources.interceptor;

import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;

/**
 * The {@link ConfigSourceInterceptor}, organised in a chain, are responsible for either resolving the configuration value
 * from the {@link ConfigSource}s or allow to intercept the resolution. It can also intercept resolution of all
 * the config property names or all values.
 * <p/>
 * <p>
 * This is useful to provide logging, Caching, transforming names or substitute values.
 * <p>
 * <p/>
 * Implementations of {@link ConfigSourceInterceptor} are loaded via the {@link java.util.ServiceLoader} mechanism and
 * can be registered by providing a resource named {@code META-INF/services/be.atbash.runtime.config.mp.sources.interceptor.ConfigSourceInterceptor},
 * which contains the fully qualified {@code ConfigSourceInterceptor} implementation class name as its content.
 * <p/>
 * <p>
 * Alternatively, a {@link ConfigSourceInterceptor} can also be loaded with a {@link ConfigSourceInterceptorFactory}.
 * <p/>
 * <p>
 * When creating a custom interceptor, one should take care to call the {@link ConfigSourceInterceptorContext} or
 * to be proper behaviour of retrieving the configuration values.
 * <p/>
 * <p>
 * The chain of interceptors will always be terminated by be.atbash.runtime.config.mp.sources.interceptor.ConfigSourceInterceptor#EMPTY.
 * <p>
 * A {@link ConfigSourceInterceptor} implementation class can specify a priority by way of the standard
 * {@code jakarta.annotation.Priority} annotation. If no priority is explicitly assigned, the default priority value
 * of {@code be.atbash.runtime.config.mp.sources.interceptor.Priorities#APPLICATION} is assumed. If multiple interceptors are registered with the
 * same priority, then their execution order may be non-deterministic.
 */
public interface ConfigSourceInterceptor extends Serializable {
    /**
     * Intercept the resolution of a configuration name and either return the corresponding {@link ConfigValue} or a
     * custom {@link ConfigValue} built by the interceptor. Calling
     * {@link ConfigSourceInterceptorContext#proceed(String)} will continue to execute the interceptor chain. The chain
     * can be short-circuited by returning another instance of {@link ConfigValue}.
     *
     * @param context the interceptor context. See {@link ConfigSourceInterceptorContext}
     * @param name    the configuration name being intercepted.
     * @return a {@link ConfigValue} with information about the name, value, config source and ordinal, or {@code null}
     * if the value isn't present.
     */
    ConfigValue getValue(ConfigSourceInterceptorContext context, String name);

    /**
     * Intercept the resolution of the configuration names. The Iterator names may be a subset of the
     * total names retrieved from all the registered ConfigSources. Calling
     * {@link ConfigSourceInterceptorContext#iterateNames()} will continue to execute the interceptor chain. The chain
     * can be short-circuited by returning another instance of the Iterator.
     *
     * @param context the interceptor context. See {@link ConfigSourceInterceptorContext}
     * @return an Iterator of Strings with configuration names.
     */
    default Iterator<String> iterateNames(ConfigSourceInterceptorContext context) {
        return context.iterateNames();
    }

    ConfigSourceInterceptor EMPTY = new ConfigSourceInterceptor() {

        @Override
        public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
            return null;
        }

        @Override
        public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
            return Collections.emptyIterator();
        }

    };
}
