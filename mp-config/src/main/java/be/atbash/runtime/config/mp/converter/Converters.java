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

import be.atbash.runtime.config.mp.util.StringUtil;
import be.atbash.util.reflection.ClassUtils;
import be.atbash.util.reflection.UnknownClassException;
import org.eclipse.microprofile.config.spi.Converter;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.function.IntFunction;
import java.util.regex.Pattern;

/**
 * General converter utilities and constants.
 * <p>
 * Based on code by Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public final class Converters {
    private Converters() {
    }

    public static final Converter<String> STRING_CONVERTER = BuiltInConverter.of(0, newEmptyValueConverter(value -> value));

    static final Converter<Boolean> BOOLEAN_CONVERTER = BuiltInConverter.of(1, newTrimmingConverter(newEmptyValueConverter(
            value -> "TRUE".equalsIgnoreCase(value)
                    || "1".equalsIgnoreCase(value)
                    || "YES".equalsIgnoreCase(value)
                    || "Y".equalsIgnoreCase(value)
                    || "ON".equalsIgnoreCase(value))));

    static final Converter<Double> DOUBLE_CONVERTER = BuiltInConverter.of(2,
            newTrimmingConverter(newEmptyValueConverter(value -> {
                try {
                    return Double.valueOf(value);
                } catch (NumberFormatException nfe) {
                    String msg = String.format("MPCONFIG-131: Expected a double value, got '%s'", value);
                    throw new NumberFormatException(msg);
                }
            })));

    static final Converter<Float> FLOAT_CONVERTER = BuiltInConverter.of(3,
            newTrimmingConverter(newEmptyValueConverter(value -> {
                try {
                    return Float.valueOf(value);
                } catch (NumberFormatException nfe) {
                    String msg = String.format("MPCONFIG-132: Expected a float value, got '%s'", value);
                    throw new NumberFormatException(msg);
                }
            })));

    static final Converter<Long> LONG_CONVERTER = BuiltInConverter.of(4,
            newTrimmingConverter(newEmptyValueConverter(value -> {
                try {
                    return Long.valueOf(value);
                } catch (NumberFormatException nfe) {
                    String msg = String.format("MPCONFIG-130: Expected a long value, got '%s'", value);
                    throw new NumberFormatException(msg);

                }
            })));

    public static final Converter<Integer> INTEGER_CONVERTER = BuiltInConverter.of(5,
            newTrimmingConverter(newEmptyValueConverter(value -> {
                try {
                    return Integer.valueOf(value);
                } catch (NumberFormatException nfe) {
                    String msg = String.format("MPCONFIG-129: Expected a integer value, got '%s'", value);
                    throw new NumberFormatException(msg);

                }
            })));

    static final Converter<Class<?>> CLASS_CONVERTER = BuiltInConverter.of(6,
            newTrimmingConverter(newEmptyValueConverter(value -> {
                try {
                    return ClassUtils.forName(value);
                } catch (UnknownClassException e) {
                    String msg = String.format("MPCONFIG-121: Converter did not find class '%s'", value);
                    throw new IllegalArgumentException(msg, e);
                }
            })));

    static final Converter<OptionalInt> OPTIONAL_INT_CONVERTER = BuiltInConverter.of(7,
            newOptionalIntConverter(INTEGER_CONVERTER));

    static final Converter<OptionalLong> OPTIONAL_LONG_CONVERTER = BuiltInConverter.of(8,
            newOptionalLongConverter(LONG_CONVERTER));

    static final Converter<OptionalDouble> OPTIONAL_DOUBLE_CONVERTER = BuiltInConverter.of(9,
            newOptionalDoubleConverter(DOUBLE_CONVERTER));

    static final Converter<InetAddress> INET_ADDRESS_CONVERTER = BuiltInConverter.of(10,
            newTrimmingConverter(newEmptyValueConverter(value -> {
                try {
                    return InetAddress.getByName(value);
                } catch (UnknownHostException e) {
                    throw new IllegalArgumentException(String.format("MPCONFIG-122: Host '%s' not found", value));
                }
            })));

    static final Converter<Character> CHARACTER_CONVERTER = BuiltInConverter.of(11, newEmptyValueConverter(value -> {
        if (value.length() == 1) {
            return value.charAt(0);
        }
        throw new IllegalArgumentException(String.format("MPCONFIG-003: '%s' can not be converted to a Character", value));
    }));

    static final Converter<Short> SHORT_CONVERTER = BuiltInConverter.of(12,
            newTrimmingConverter(newEmptyValueConverter(Short::valueOf)));

    static final Converter<Byte> BYTE_CONVERTER = BuiltInConverter.of(13,
            newTrimmingConverter(newEmptyValueConverter(Byte::valueOf)));

    static final Converter<UUID> UUID_CONVERTER = BuiltInConverter.of(14,
            newTrimmingConverter(newEmptyValueConverter((s) -> {
                try {
                    return UUID.fromString(s);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(String.format("MPCONFIG-026: '%s' cannot be converted into a UUID", s));
                }
            })));

    static final Converter<Currency> CURRENCY_CONVERTER = BuiltInConverter.of(15,
            newTrimmingConverter(newEmptyValueConverter(Currency::getInstance)));

    static final Converter<BitSet> BITSET_CONVERTER = BuiltInConverter.of(16,
            newTrimmingConverter((s) -> {
                int len = s.length();
                byte[] data = new byte[len / 2];
                for (int i = 0; i < len; i += 2) {
                    data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                            + Character.digit(s.charAt(i + 1), 16));
                }
                return BitSet.valueOf(data);
            }));

    static final Converter<Pattern> PATTERN_CONVERTER = BuiltInConverter.of(17,
            newTrimmingConverter(newEmptyValueConverter(Pattern::compile)));

    public static final Map<Class<?>, Class<?>> PRIMITIVE_TYPES;

    public static final Map<Type, Converter<?>> ALL_CONVERTERS = new HashMap<>();

    static {
        ALL_CONVERTERS.put(String.class, STRING_CONVERTER);

        ALL_CONVERTERS.put(Boolean.class, BOOLEAN_CONVERTER);

        ALL_CONVERTERS.put(Double.class, DOUBLE_CONVERTER);

        ALL_CONVERTERS.put(Float.class, FLOAT_CONVERTER);

        ALL_CONVERTERS.put(Long.class, LONG_CONVERTER);

        ALL_CONVERTERS.put(Integer.class, INTEGER_CONVERTER);

        ALL_CONVERTERS.put(Short.class, SHORT_CONVERTER);

        ALL_CONVERTERS.put(Class.class, CLASS_CONVERTER);
        ALL_CONVERTERS.put(InetAddress.class, INET_ADDRESS_CONVERTER);

        ALL_CONVERTERS.put(OptionalInt.class, OPTIONAL_INT_CONVERTER);
        ALL_CONVERTERS.put(OptionalLong.class, OPTIONAL_LONG_CONVERTER);
        ALL_CONVERTERS.put(OptionalDouble.class, OPTIONAL_DOUBLE_CONVERTER);

        ALL_CONVERTERS.put(Character.class, CHARACTER_CONVERTER);

        ALL_CONVERTERS.put(Byte.class, BYTE_CONVERTER);

        ALL_CONVERTERS.put(UUID.class, UUID_CONVERTER);

        ALL_CONVERTERS.put(Currency.class, CURRENCY_CONVERTER);

        ALL_CONVERTERS.put(BitSet.class, BITSET_CONVERTER);

        ALL_CONVERTERS.put(Pattern.class, PATTERN_CONVERTER);

        PRIMITIVE_TYPES = new HashMap<>(9);
        PRIMITIVE_TYPES.put(byte.class, Byte.class);
        PRIMITIVE_TYPES.put(short.class, Short.class);
        PRIMITIVE_TYPES.put(int.class, Integer.class);
        PRIMITIVE_TYPES.put(long.class, Long.class);

        PRIMITIVE_TYPES.put(float.class, Float.class);
        PRIMITIVE_TYPES.put(double.class, Double.class);

        PRIMITIVE_TYPES.put(char.class, Character.class);

        PRIMITIVE_TYPES.put(boolean.class, Boolean.class);

        PRIMITIVE_TYPES.put(void.class, Void.class);
    }

    public static Class<?> wrapPrimitiveType(Class<?> primitiveType) {
        assert primitiveType.isPrimitive();
        return PRIMITIVE_TYPES.get(primitiveType);
    }

    /**
     * Get the type of the converter specified by {@code clazz}. If the given class is not a valid
     * converter, then {@code null} is returned.
     *
     * @param clazz the converter class (must not be {@code null})
     * @return the converter target type, or {@code null} if the class does not represent a valid configuration
     * @throws IllegalStateException if the given converter class is not properly parameterized
     */
    public static Type getConverterType(Class<?> clazz) {
        if (clazz.equals(Object.class)) {
            return null;
        }

        for (Type type : clazz.getGenericInterfaces()) {
            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                if (pt.getRawType().equals(Converter.class)) {
                    Type actualTypeArgument = pt.getActualTypeArguments()[0];

                    if (actualTypeArgument instanceof Class) {
                        return actualTypeArgument;
                    }
                    if (actualTypeArgument instanceof ParameterizedType) {
                        return ((ParameterizedType) actualTypeArgument).getRawType();
                    }

                }
            }
        }

        return getConverterType(clazz.getSuperclass());
    }

    /**
     * Get a converter that converts a comma-separated string into a list of converted items.
     *
     * @param itemConverter     the item converter (must not be {@code null})
     * @param collectionFactory the collection factory (must not be {@code null})
     * @param <T>               the item type
     * @param <C>               the collection type
     * @return the new converter (not {@code null})
     */
    public static <T, C extends Collection<T>> Converter<C> newCollectionConverter(Converter<? extends T> itemConverter,
                                                                                   IntFunction<C> collectionFactory) {
        return new CollectionConverter<>(itemConverter, collectionFactory);
    }

    /**
     * Get a converter that converts a comma-separated string into an array of converted items.
     *
     * @param itemConverter the item converter (must not be {@code null})
     * @param arrayType     the array type class (must not be {@code null})
     * @param <T>           the item type
     * @param <A>           the array type
     * @return the new converter (not {@code null})
     */
    public static <A, T> Converter<A> newArrayConverter(Converter<? extends T> itemConverter, Class<A> arrayType) {
        if (!arrayType.isArray()) {
            throw new IllegalArgumentException(String.format("MPCONFIG-004: %s is not an array type", arrayType));
        }
        return new ArrayConverter<>(itemConverter, arrayType);
    }

    /**
     * Get a converter which wraps another converter's result into an {@code Optional}. If the delegate converter
     * returns {@code null}, this converter returns {@link Optional#empty()}.
     *
     * @param delegateConverter the delegate converter (must not be {@code null})
     * @param <T>               the item type
     * @return the new converter (not {@code null})
     */
    public static <T> Converter<Optional<T>> newOptionalConverter(Converter<? extends T> delegateConverter) {
        return new OptionalConverter<>(delegateConverter);
    }

    /**
     * Get a converter which wraps another converter's result into an {@code OptionalInt}. If the delegate converter
     * returns {@code null}, this converter returns {@link Optional#empty()}.
     *
     * @param delegateConverter the delegate converter (must not be {@code null})
     * @return the new converter (not {@code null})
     */
    public static Converter<OptionalInt> newOptionalIntConverter(Converter<Integer> delegateConverter) {
        return new OptionalIntConverter(delegateConverter);
    }

    /**
     * Get a converter which wraps another converter's result into an {@code OptionalLong}. If the delegate converter
     * returns {@code null}, this converter returns {@link Optional#empty()}.
     *
     * @param delegateConverter the delegate converter (must not be {@code null})
     * @return the new converter (not {@code null})
     */
    public static Converter<OptionalLong> newOptionalLongConverter(Converter<Long> delegateConverter) {
        return new OptionalLongConverter(delegateConverter);
    }

    /**
     * Get a converter which wraps another converter's result into an {@code OptionalDouble}. If the delegate converter
     * returns {@code null}, this converter returns {@link Optional#empty()}.
     *
     * @param delegateConverter the delegate converter (must not be {@code null})
     * @return the new converter (not {@code null})
     */
    public static Converter<OptionalDouble> newOptionalDoubleConverter(Converter<Double> delegateConverter) {
        return new OptionalDoubleConverter(delegateConverter);
    }

    /**
     * Get a converter which wraps another converter and handles empty values correctly. This allows the
     * delegate converter to assume that the value being converted will not be {@code null} or empty.
     *
     * @param delegateConverter the converter to delegate to (must not be {@code null})
     * @param <T>               the value type
     * @return the converter
     */
    public static <T> Converter<T> newEmptyValueConverter(Converter<T> delegateConverter) {
        return new EmptyValueConverter<>(delegateConverter, null);
    }

    /**
     * Get a converter which trims the string input before passing it on to the delegate converter.
     *
     * @param delegateConverter the converter to delegate to (must not be {@code null})
     * @param <T>               the value type
     * @return the converter
     */
    public static <T> Converter<T> newTrimmingConverter(Converter<T> delegateConverter) {
        return new TrimmingConverter<>(delegateConverter);
    }


    static final class CollectionConverter<T, C extends Collection<T>> extends AbstractDelegatingConverter<T, C> {
        private static final long serialVersionUID = -8452214026800305628L;

        private final IntFunction<C> collectionFactory;

        CollectionConverter(Converter<? extends T> delegate, IntFunction<C> collectionFactory) {
            super(delegate);
            this.collectionFactory = collectionFactory;
        }

        public C convert(String str) {
            if (str.isEmpty()) {
                // empty collection
                return null;
            }
            String[] itemStrings = StringUtil.split(str);
            C collection = collectionFactory.apply(itemStrings.length);
            for (String itemString : itemStrings) {
                if (!itemString.isEmpty()) {
                    T item = getDelegate().convert(itemString);
                    if (item != null) {
                        collection.add(item);
                    }
                }
            }
            return collection.isEmpty() ? null : collection;
        }
    }

    static final class ArrayConverter<T, A> extends AbstractDelegatingConverter<T, A> {
        private static final long serialVersionUID = 2630282286159527380L;

        private final Class<A> arrayType;

        ArrayConverter(Converter<? extends T> delegate, Class<A> arrayType) {
            super(delegate);
            this.arrayType = arrayType;
        }

        public A convert(String str) {
            if (str.isEmpty()) {
                // empty array
                return null;
            }
            String[] itemStrings = StringUtil.split(str);
            A array = arrayType.cast(Array.newInstance(arrayType.getComponentType(), itemStrings.length));
            int size = 0;
            for (String itemString : itemStrings) {
                if (!itemString.isEmpty()) {
                    T item = getDelegate().convert(itemString);
                    if (item != null) {
                        Array.set(array, size++, item);
                    }
                }
            }
            return size == 0 ? null : size < itemStrings.length ? copyArray(array, arrayType, size) : array;
        }

        private static <A> A copyArray(A array, Class<A> arrayType, int newSize) {
            if (array instanceof Object[]) {
                return arrayType.cast(Arrays.copyOf((Object[]) array, newSize));
            } else if (array instanceof boolean[]) {
                return arrayType.cast(Arrays.copyOf((boolean[]) array, newSize));
            } else if (array instanceof char[]) {
                return arrayType.cast(Arrays.copyOf((char[]) array, newSize));
            } else if (array instanceof byte[]) {
                return arrayType.cast(Arrays.copyOf((byte[]) array, newSize));
            } else if (array instanceof short[]) {
                return arrayType.cast(Arrays.copyOf((short[]) array, newSize));
            } else if (array instanceof int[]) {
                return arrayType.cast(Arrays.copyOf((int[]) array, newSize));
            } else if (array instanceof long[]) {
                return arrayType.cast(Arrays.copyOf((long[]) array, newSize));
            } else if (array instanceof float[]) {
                return arrayType.cast(Arrays.copyOf((float[]) array, newSize));
            } else if (array instanceof double[]) {
                return arrayType.cast(Arrays.copyOf((double[]) array, newSize));
            } else {
                throw new IllegalArgumentException("MPCONFIG-023: Array type being converted is unknown");
            }
        }
    }

    public static final class OptionalConverter<T> extends AbstractDelegatingConverter<T, Optional<T>> {
        private static final long serialVersionUID = -4051551570591834428L;

        OptionalConverter(Converter<? extends T> delegate) {
            super(delegate);
        }

        public Optional<T> convert(String value) {
            if (value.isEmpty()) {
                try {
                    return Optional.ofNullable(getDelegate().convert(value));
                } catch (IllegalArgumentException ignored) {
                    return Optional.empty();
                }
            } else {
                return Optional.ofNullable(getDelegate().convert(value));
            }
        }
    }

    static final class OptionalIntConverter extends AbstractDelegatingConverter<Integer, OptionalInt> {

        private OptionalIntConverter(Converter<? extends Integer> delegate) {
            super(delegate);
        }

        public OptionalInt convert(String value) {
            if (value.isEmpty()) {
                return OptionalInt.empty();
            } else {
                Integer converted = getDelegate().convert(value);
                return converted == null ? OptionalInt.empty() : OptionalInt.of(converted.intValue());
            }
        }
    }

    static final class OptionalLongConverter extends AbstractDelegatingConverter<Long, OptionalLong> {
        private static final long serialVersionUID = 140937551800590852L;

        private OptionalLongConverter(Converter<? extends Long> delegate) {
            super(delegate);
        }

        public OptionalLong convert(String value) {
            if (value.isEmpty()) {
                return OptionalLong.empty();
            } else {
                Long converted = getDelegate().convert(value);
                return converted == null ? OptionalLong.empty() : OptionalLong.of(converted.longValue());
            }
        }
    }

    static final class OptionalDoubleConverter extends AbstractDelegatingConverter<Double, OptionalDouble> {
        private static final long serialVersionUID = -2882741842811044902L;

        OptionalDoubleConverter(Converter<? extends Double> delegate) {
            super(delegate);
        }

        public OptionalDouble convert(String value) {
            if (value.isEmpty()) {
                return OptionalDouble.empty();
            } else {
                Double converted = getDelegate().convert(value);
                return converted == null ? OptionalDouble.empty() : OptionalDouble.of(converted.doubleValue());
            }
        }
    }

    static final class BuiltInConverter<T> implements Converter<T>, Serializable {
        private final int id;
        private final Converter<T> function;

        static <T> BuiltInConverter<T> of(int id, Converter<T> function) {
            return new BuiltInConverter<>(id, function);
        }

        private BuiltInConverter(int id, Converter<T> function) {
            this.id = id;
            this.function = function;
        }

        public T convert(String value) {
            return function.convert(value);
        }

        Object writeReplace() {
            return new Ser(id);
        }
    }

    static final class Ser implements Serializable {
        private static final long serialVersionUID = 5646753664957303950L;

        private final short id;

        Ser(int id) {
            this.id = (short) id;
        }

        Object readResolve() throws ObjectStreamException {
            switch (id) {
                case 0:
                    return STRING_CONVERTER;
                case 1:
                    return BOOLEAN_CONVERTER;
                case 2:
                    return DOUBLE_CONVERTER;
                case 3:
                    return FLOAT_CONVERTER;
                case 4:
                    return LONG_CONVERTER;
                case 5:
                    return INTEGER_CONVERTER;
                case 6:
                    return CLASS_CONVERTER;
                case 7:
                    return OPTIONAL_INT_CONVERTER;
                case 8:
                    return OPTIONAL_LONG_CONVERTER;
                case 9:
                    return OPTIONAL_DOUBLE_CONVERTER;
                case 10:
                    return INET_ADDRESS_CONVERTER;
                case 11:
                    return CHARACTER_CONVERTER;
                case 12:
                    return SHORT_CONVERTER;
                case 13:
                    return BYTE_CONVERTER;
                case 14:
                    return UUID_CONVERTER;
                case 15:
                    return CURRENCY_CONVERTER;
                case 16:
                    return BITSET_CONVERTER;
                case 17:
                    return PATTERN_CONVERTER;
                default:
                    String msg = String.format("MPCONFIG-010: Unknown converter ID: %s", id);
                    throw new InvalidObjectException(msg);
            }
        }
    }

    static class EmptyValueConverter<T> extends AbstractDelegatingConverter<T, T> {
        private static final long serialVersionUID = 5607979836385662739L;

        private final T emptyValue;

        EmptyValueConverter(Converter<? extends T> delegate, T emptyValue) {
            super(delegate);
            this.emptyValue = emptyValue;
        }

        protected EmptyValueConverter<T> create(Converter<? extends T> newDelegate) {
            return new EmptyValueConverter<>(newDelegate, emptyValue);
        }

        public T convert(String value) {
            if (value.isEmpty()) {
                return emptyValue;
            }
            T result = getDelegate().convert(value);
            if (result == null) {
                return emptyValue;
            } else {
                return result;
            }
        }
    }

    static class TrimmingConverter<T> extends AbstractDelegatingConverter<T, T> {

        TrimmingConverter(Converter<? extends T> delegate) {
            super(delegate);
        }

        protected TrimmingConverter<T> create(Converter<? extends T> newDelegate) {
            return new TrimmingConverter<>(newDelegate);
        }

        public T convert(String value) {
            if (value == null) {
                throw new NullPointerException("The Converter API cannot convert a null value");
            }
            return getDelegate().convert(value.trim());
        }
    }
}
