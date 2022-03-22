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

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.*;

/**
 * Based on GERONIMO-6595 support implicit converters.
 * <p>
 * Based on code by Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class ImplicitConverters {

    private ImplicitConverters() {
    }

    public static <T> Converter<T> getConverter(Class<? extends T> clazz) {
        // implicit converters required by the specification
        Converter<T> converter = getConverterFromStaticMethod(clazz, "of", String.class);
        if (converter == null) {
            converter = getConverterFromStaticMethod(clazz, "of", CharSequence.class);
        }
        if (converter == null) {
            converter = getConverterFromStaticMethod(clazz, "valueOf", String.class);
        }
        if (converter == null) {
            converter = getConverterFromStaticMethod(clazz, "valueOf", CharSequence.class);
        }
        if (converter == null) {
            converter = getConverterFromStaticMethod(clazz, "parse", String.class);
        }
        if (converter == null) {
            converter = getConverterFromStaticMethod(clazz, "parse", CharSequence.class);
        }
        if (converter == null) {
            converter = getConverterFromConstructor(clazz, String.class);
        }
        if (converter == null) {
            converter = getConverterFromConstructor(clazz, CharSequence.class);
        }
        return converter;
    }

    private static <T> Converter<T> getConverterFromConstructor(Class<? extends T> clazz, Class<? super String> paramType) {
        try {
            Constructor<? extends T> declaredConstructor = clazz.getDeclaredConstructor(paramType);
            if (!isAccessible(declaredConstructor)) {
                declaredConstructor.setAccessible(true);
            }
            return new ConstructorConverter<>(declaredConstructor);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static <T> Converter<T> getConverterFromStaticMethod(Class<? extends T> clazz, String methodName,
                                                                 Class<? super String> paramType) {
        try {
            Method method = clazz.getMethod(methodName, paramType);
            if (clazz != method.getReturnType()) {
                // doesn't meet requirements of the spec
                return null;
            }
            if (!Modifier.isStatic(method.getModifiers())) {
                return null;
            }
            if (!isAccessible(method)) {
                method.setAccessible(true);
            }
            return new StaticMethodConverter<>(clazz, method);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static boolean isAccessible(Executable e) {
        return Modifier.isPublic(e.getModifiers()) && Modifier.isPublic(e.getDeclaringClass().getModifiers()) ||
                e.isAccessible();
    }

    static class StaticMethodConverter<T> implements Converter<T>, Serializable {

        private static final long serialVersionUID = 3350265927359848883L;

        private final Class<? extends T> clazz;
        private final Method method;

        StaticMethodConverter(Class<? extends T> clazz, Method method) {
            assert clazz == method.getReturnType();
            this.clazz = clazz;
            this.method = method;
        }

        @Override
        public T convert(String value) {
            if (value.isEmpty()) {
                return null;
            }
            try {
                return clazz.cast(method.invoke(null, value));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalArgumentException("MPCONFIG-020: Failed to convert value with static method");
            }
        }

        Object writeReplace() {
            return new Serialized(method.getDeclaringClass());
        }

        static final class Serialized implements Serializable {
            private static final long serialVersionUID = -6334004040897615452L;

            private final Class<?> c;


            Serialized(Class<?> c) {
                this.c = c;
            }

            Object readResolve() throws ObjectStreamException {
                return getConverter(c);
            }
        }
    }

    static class ConstructorConverter<T> implements Converter<T>, Serializable {


        private final Constructor<? extends T> ctor;

        public ConstructorConverter(final Constructor<? extends T> ctor) {
            this.ctor = ctor;
        }

        @Override
        public T convert(String value) {
            if (value.isEmpty()) {
                return null;
            }
            try {
                return ctor.newInstance(value);
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {

                throw new IllegalArgumentException("MPCONFIG-019: Failed to create new instance from Converter constructor");

            }
        }

        Object writeReplace() {
            return new Serialized(ctor.getDeclaringClass(), ctor.getParameterTypes()[0]);
        }

        static final class Serialized implements Serializable {
            private static final long serialVersionUID = -2903564775826815453L;

            private final Class<?> c;
            @SuppressWarnings("unused")
            private final Class<?> p;

            Serialized(Class<?> c, Class<?> p) {
                this.c = c;
                this.p = p;
            }

            Object readResolve() throws ObjectStreamException {
                return getConverter(c);
            }
        }
    }
}
