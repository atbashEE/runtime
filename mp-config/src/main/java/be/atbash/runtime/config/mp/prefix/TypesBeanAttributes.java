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

import jakarta.enterprise.inject.spi.BeanAttributes;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * A simple Bean Attributes delegate that delegates to the passed in BeanAttributes
 * Does not implement getTypes to allow overriding
 * This class is used to create synthetic producer beans for each Converter registered
 * with the Config
 * <p>
 * Based on code by Steve Millidge (Payara Foundation)
 */
public abstract class TypesBeanAttributes<T> implements BeanAttributes<T> {

    private final BeanAttributes<?> delegate;

    public TypesBeanAttributes(BeanAttributes<?> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return delegate.getQualifiers();
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return delegate.getScope();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return delegate.getStereotypes();
    }

    @Override
    public boolean isAlternative() {
        return delegate.isAlternative();
    }

}
