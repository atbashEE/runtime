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
package be.atbash.runtime.config.mp.converter;

import org.eclipse.microprofile.config.spi.Converter;

/**
 *
 * A converter which wraps another converter (possibly of a different type). Used in the Optional and collection type converters.
 * <p/>
 * Based on code from SmallRye Config.
 */
public abstract class AbstractDelegatingConverter<I, O> extends AbstractConverter<O> {

    private final Converter<? extends I> delegate;

    protected AbstractDelegatingConverter(final Converter<? extends I> delegate) {
        this.delegate = delegate;
    }

    public Converter<? extends I> getDelegate() {
        return delegate;
    }
}
