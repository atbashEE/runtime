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

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.Converter;

import java.util.NoSuchElementException;

/**
 *
 */
public final class ConvertValueUtil {

    private ConvertValueUtil() {

    }

    /**
     * This method handles converting values for both CDI injections and programatical calls.<br>
     * <br>
     * <p>
     * Calls for converting non-optional values ({@link Config#getValue} and "Injecting Native Values")
     * should throw an {@link Exception} for each of the following:<br>
     * <p>
     * 1. {@link IllegalArgumentException} - if the property cannot be converted by the {@link Converter} to the specified type
     * <br>
     * 2. {@link NoSuchElementException} - if the property is not defined <br>
     * 3. {@link NoSuchElementException} - if the property is defined as an empty string <br>
     * 4. {@link NoSuchElementException} - if the {@link Converter} returns {@code null} <br>
     * <br>
     * <p>
     * Calls for converting optional values ({@link Config#getOptionalValue} and "Injecting Optional Values")
     * should only throw an {@link Exception} for #1 ({@link IllegalArgumentException} when the property cannot be converted to
     * the specified type).
     */
    public static <T> T convertValue(String name, String value, Converter<T> converter) {

        T converted;

        if (value != null) {
            try {
                converted = converter.convert(value);
            } catch (IllegalArgumentException ex) {
                String msg = String.format("MPCONFIG-139: The config property '%s' with the config value '%s' threw an Exception whilst being converted %s"
                        , name, value, ex.getLocalizedMessage());
                throw new IllegalArgumentException(msg, ex);  // 1

            }
        } else {
            try {
                // See if the Converter is designed to handle a missing (null) value i.e. Optional Converters
                converted = converter.convert("");
            } catch (IllegalArgumentException ex) {
                String msg = String.format("MPCONFIG-114: The config property '%s' is required but it could not be found in any config source", name);
                throw new NoSuchElementException(msg); // 2
            }
        }

        if (converted == null) {
            if (value == null) {
                String msg = String.format("MPCONFIG-114: The config property '%s' is required but it could not be found in any config source", name);
                throw new NoSuchElementException(msg); // 2
            } else if (value.length() == 0) {
                String msg = String.format("MPCONFIG-140: The config property '%s' is defined as the empty String (\"\") which the following Converter considered to be null: %s", name, converter.getClass().getTypeName());
                throw new NoSuchElementException(msg); // 3
            } else {
                String msg = String.format("MPCONFIG-141: The config property '%s' with the config value '%s' was converted to null from the following Converter: %s", name, value, converter.getClass().getTypeName());

                throw new NoSuchElementException(msg); // 4
            }
        }

        return converted;
    }
}
