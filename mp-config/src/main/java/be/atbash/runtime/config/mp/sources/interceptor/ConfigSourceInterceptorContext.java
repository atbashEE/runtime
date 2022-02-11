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

import java.io.Serializable;
import java.util.Iterator;

/**
 * Exposes contextual information about the intercepted invocation of {@link ConfigSourceInterceptor}. This allows
 * implementers to control the behavior of the invocation chain.
 */
public interface ConfigSourceInterceptorContext extends Serializable {
    /**
     * Proceeds to the next interceptor in the chain.
     *
     * @param name the configuration name to lookup. Can be the original key.
     * @return a {@link ConfigValue} with information about the name, value, config source and ordinal, or {@code null}
     * if the value isn't present.
     */
    ConfigValue proceed(String name);

    /**
     * Proceeds to the next interceptor in the chain.
     *
     * @return an Iterator of Strings with configuration names.
     */
    Iterator<String> iterateNames();

}
