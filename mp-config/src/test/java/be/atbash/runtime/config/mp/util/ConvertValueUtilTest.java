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
package be.atbash.runtime.config.mp.util;

import be.atbash.runtime.config.mp.converter.Converters;
import be.atbash.runtime.config.mp.converter.testclass.ToNullTestConverter;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.OptionalInt;

class ConvertValueUtilTest {

    @Test
    void convertValue() {

        Integer value = ConvertValueUtil.convertValue("test", "1234", Converters.INTEGER_CONVERTER);
        Assertions.assertThat(value).isEqualTo(1234);
    }

    @Test
    void convertValue_convertError() {

        IllegalArgumentException exception = Assertions.catchThrowableOfType(() ->
                        ConvertValueUtil.convertValue("test", "ABC", Converters.INTEGER_CONVERTER)
                , IllegalArgumentException.class);
        Assertions.assertThat(exception.getMessage()).isEqualTo("MPCONFIG-139: The config property 'test' with the config value 'ABC' threw an Exception whilst being converted MPCONFIG-129: Expected a integer value, got 'ABC'");

    }

    @Test
    void convertValue_optional() {

        OptionalInt value = ConvertValueUtil.convertValue("test", null, Converters.newOptionalIntConverter(Converters.INTEGER_CONVERTER));
        Assertions.assertThat(value).isEmpty();
    }

    @Test
    void convertValue_convertToNull() {
        NoSuchElementException exception = Assertions.catchThrowableOfType(() ->
                        ConvertValueUtil.convertValue("test", "Atbash", new ToNullTestConverter())
                , NoSuchElementException.class);
        Assertions.assertThat(exception.getMessage()).isEqualTo("MPCONFIG-141: The config property 'test' with the config value 'Atbash' was converted to null from the following Converter: be.atbash.runtime.config.mp.converter.testclass.ToNullTestConverter");
    }

    @Test
    void convertValue_convertToEmpty() {
        NoSuchElementException exception = Assertions.catchThrowableOfType(() ->
                        ConvertValueUtil.convertValue("test", "", Converters.INTEGER_CONVERTER)
                , NoSuchElementException.class);
        Assertions.assertThat(exception.getMessage()).isEqualTo("MPCONFIG-140: The config property 'test' is defined as the empty String (\"\") which the following Converter considered to be null: be.atbash.runtime.config.mp.converter.Converters$BuiltInConverter");
    }

    @Test
    void convertValue_nullValue() {
        NoSuchElementException exception = Assertions.catchThrowableOfType(() ->
                        ConvertValueUtil.convertValue("test", null, Converters.INTEGER_CONVERTER)
                , NoSuchElementException.class);
        Assertions.assertThat(exception.getMessage()).isEqualTo("MPCONFIG-114: The config property 'test' is required but it could not be found in any config source");
    }

    @Test
    void convertValue_nullValueWithException() {
        NoSuchElementException exception = Assertions.catchThrowableOfType(() ->
                        ConvertValueUtil.convertValue("test", null, new ToNullTestConverter())
                , NoSuchElementException.class);
        Assertions.assertThat(exception.getMessage()).isEqualTo("MPCONFIG-114: The config property 'test' is required but it could not be found in any config source");
    }


}