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

import be.atbash.runtime.config.mp.converter.testclass.DoubleParameterizedTypeClass;
import be.atbash.runtime.config.mp.converter.testclass.NotParameterizedInterface;
import be.atbash.runtime.config.mp.converter.testclass.SomeClassConverter1;
import be.atbash.runtime.config.mp.converter.testclass.SomeParameterizedInterface;
import be.atbash.runtime.config.mp.util.testclass.SomeClass;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.net.InetAddress;
import java.util.UUID;

class ConvertersTest {

    @Test
    void getConverterType_noInterfaces() {
        Type type = Converters.getConverterType(SomeClass.class);
        Assertions.assertThat(type).isNull();
    }

    @Test
    void getConverterType_notParameterizedInterface() {
        Type type = Converters.getConverterType(NotParameterizedInterface.class);
        Assertions.assertThat(type).isNull();
    }

    @Test
    void getConverterType_parameterizedNotConverter() {
        Type type = Converters.getConverterType(SomeParameterizedInterface.class);
        Assertions.assertThat(type).isNull();
    }

    @Test
    void getConverterType_DoubleParameterizedTypeWithConverter() {
        Type type = Converters.getConverterType(DoubleParameterizedTypeClass.class);
        Assertions.assertThat(type).isNull();  // It cannot find out the Converter is for Type String.
        // This kind of constructs need to be registered by specifying the type.
    }

    @Test
    void getConverterType_correctClass() {
        Type type = Converters.getConverterType(SomeClassConverter1.class);
        Assertions.assertThat(type).isNotNull();
        Assertions.assertThat(type).isEqualTo(SomeClass.class);
    }

    @Test
    void booleanConverter() {
        Object converted = Converters.ALL_CONVERTERS.get(Boolean.class).convert("TRUE");
        Assertions.assertThat(converted).isInstanceOf(Boolean.class);
        Assertions.assertThat((Boolean) converted).isTrue();
    }

    @Test
    void booleanConverter_spacesLowercase() {

        Object converted = Converters.ALL_CONVERTERS.get(Boolean.class).convert("  true  ");
        Assertions.assertThat(converted).isInstanceOf(Boolean.class);
        Assertions.assertThat((Boolean) converted).isTrue();
    }

    @Test
    void doubleConverter() {
        Object converted = Converters.ALL_CONVERTERS.get(Double.class).convert("1234.56");
        Assertions.assertThat(converted).isInstanceOf(Double.class);
        Assertions.assertThat((Double) converted).isCloseTo(1234.56, Offset.offset(0.001));
    }

    @Test
    void doubleConverter_spaces() {

        Object converted = Converters.ALL_CONVERTERS.get(Double.class).convert("  65.4321  ");
        Assertions.assertThat(converted).isInstanceOf(Double.class);
        Assertions.assertThat((Double) converted).isCloseTo(65.4321, Offset.offset(0.00001));
    }

    @Test
    void doubleConverter_incorrectFormat() {
        NumberFormatException thrownException = Assertions.catchThrowableOfType(
                () -> Converters.ALL_CONVERTERS.get(Double.class).convert("1234D.56")
                , NumberFormatException.class);
        Assertions.assertThat(thrownException.getMessage()).isEqualTo("MPCONFIG-131: Expected a double value, got '1234D.56'");
    }

    @Test
    void floatConverter() {
        Object converted = Converters.ALL_CONVERTERS.get(Float.class).convert("1234.56");
        Assertions.assertThat(converted).isInstanceOf(Float.class);
        Assertions.assertThat((Float) converted).isCloseTo(1234.56F, Offset.offset(0.001F));
    }

    @Test
    void floatConverter_spaces() {

        Object converted = Converters.ALL_CONVERTERS.get(Float.class).convert("  65.4321  ");
        Assertions.assertThat(converted).isInstanceOf(Float.class);
        Assertions.assertThat((Float) converted).isCloseTo(65.4321F, Offset.offset(0.00001F));
    }

    @Test
    void floatConverter_incorrectFormat() {
        NumberFormatException thrownException = Assertions.catchThrowableOfType(
                () -> Converters.ALL_CONVERTERS.get(Float.class).convert("1234D.56")
                , NumberFormatException.class);
        Assertions.assertThat(thrownException.getMessage()).isEqualTo("MPCONFIG-132: Expected a float value, got '1234D.56'");
    }

    @Test
    void longConverter() {
        Object converted = Converters.ALL_CONVERTERS.get(Long.class).convert("1234567890");
        Assertions.assertThat(converted).isInstanceOf(Long.class);
        Assertions.assertThat((Long) converted).isEqualTo(1234567890L);
    }

    @Test
    void longConverter_spaces() {

        Object converted = Converters.ALL_CONVERTERS.get(Long.class).convert("  1234567890  ");
        Assertions.assertThat(converted).isInstanceOf(Long.class);
        Assertions.assertThat((Long) converted).isEqualTo(1234567890L);
    }

    @Test
    void longConverter_incorrectFormat() {
        NumberFormatException thrownException = Assertions.catchThrowableOfType(
                () -> Converters.ALL_CONVERTERS.get(Long.class).convert("1234.5")
                , NumberFormatException.class);
        Assertions.assertThat(thrownException.getMessage()).isEqualTo("MPCONFIG-130: Expected a long value, got '1234.5'");
    }


    @Test
    void integerConverter() {
        Object converted = Converters.ALL_CONVERTERS.get(Integer.class).convert("-123");
        Assertions.assertThat(converted).isInstanceOf(Integer.class);
        Assertions.assertThat((Integer) converted).isEqualTo(-123);
    }

    @Test
    void integerConverter_spaces() {

        Object converted = Converters.ALL_CONVERTERS.get(Integer.class).convert("  -123  ");
        Assertions.assertThat(converted).isInstanceOf(Integer.class);
        Assertions.assertThat((Integer) converted).isEqualTo(-123);
    }

    @Test
    void integerConverter_incorrectFormat() {
        NumberFormatException thrownException = Assertions.catchThrowableOfType(
                () -> Converters.ALL_CONVERTERS.get(Integer.class).convert("1234.5")
                , NumberFormatException.class);
        Assertions.assertThat(thrownException.getMessage()).isEqualTo("MPCONFIG-129: Expected a integer value, got '1234.5'");
    }


    @Test
    void classConverter() {
        Object converted = Converters.ALL_CONVERTERS.get(Class.class).convert(SomeClass.class.getName());
        Assertions.assertThat(converted).isInstanceOf(Class.class);

        Assertions.assertThat((Class<?>) converted).isEqualTo(SomeClass.class);

    }

    @Test
    void classConverter_unknown() {
        IllegalArgumentException thrownException = Assertions.catchThrowableOfType(
                () -> Converters.ALL_CONVERTERS.get(Class.class).convert("some.random.class.name")
                , IllegalArgumentException.class);

        Assertions.assertThat(thrownException.getMessage()).isEqualTo("MPCONFIG-121: Converter did not find class 'some.random.class.name'");
    }

    @Test
    void inetAddressConverter() {
        Object converted = Converters.ALL_CONVERTERS.get(InetAddress.class).convert("localhost");
        Assertions.assertThat(converted).isInstanceOf(InetAddress.class);

        InetAddress address = (InetAddress) converted;
        Assertions.assertThat(address.getHostAddress()).isEqualTo("127.0.0.1");

    }

    @Test
    void inetAddressConverter_unknown() {

        IllegalArgumentException thrownException = Assertions.catchThrowableOfType(
                () -> Converters.ALL_CONVERTERS.get(InetAddress.class).convert("AtbashServer")
                , IllegalArgumentException.class);
        Assertions.assertThat(thrownException.getMessage()).isEqualTo("MPCONFIG-122: Host 'AtbashServer' not found");
    }

    @Test
    void inetAddressConverter_empty() {

        Object converted = Converters.ALL_CONVERTERS.get(InetAddress.class).convert("");
        Assertions.assertThat(converted).isNull();


    }

    @Test
    void characterConverter() {
        Object converted = Converters.ALL_CONVERTERS.get(Character.class).convert("A");
        Assertions.assertThat(converted).isInstanceOf(Character.class);

        Assertions.assertThat((Character) converted).isEqualTo('A');

    }

    @Test
    void characterConverter_incorrect() {
        IllegalArgumentException thrownException = Assertions.catchThrowableOfType(
                () -> Converters.ALL_CONVERTERS.get(Character.class).convert("Atbash")
                , IllegalArgumentException.class);
        Assertions.assertThat(thrownException.getMessage()).isEqualTo("MPCONFIG-003: 'Atbash' can not be converted to a Character");

    }

    @Test
    void characterConverter_emptyValue() {
        Object converted = Converters.ALL_CONVERTERS.get(Character.class).convert("");
        Assertions.assertThat(converted).isNull();

    }

    @Test
    void uuidConverter() {
        UUID uuid = UUID.randomUUID();
        Object converted = Converters.ALL_CONVERTERS.get(UUID.class).convert(uuid.toString());
        Assertions.assertThat(converted).isInstanceOf(UUID.class);

        Assertions.assertThat(converted.toString()).isEqualTo(uuid.toString());

    }

    @Test
    void uuidConverter_incorrectValue() {
        IllegalArgumentException thrownException = Assertions.catchThrowableOfType(
                () -> Converters.ALL_CONVERTERS.get(UUID.class).convert("something that is not UUID")
                , IllegalArgumentException.class);
        Assertions.assertThat(thrownException.getMessage()).isEqualTo("MPCONFIG-026: 'something that is not UUID' cannot be converted into a UUID");

    }


}