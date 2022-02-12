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
package be.atbash.runtime.config.mp.converter.testclass;

import be.atbash.runtime.config.mp.util.testclass.SomeClass;
import org.eclipse.microprofile.config.spi.Converter;


public class SomeClassConverter2 implements Converter<SomeClass> {
    @Override
    public SomeClass convert(String value) throws IllegalArgumentException, NullPointerException {
        if (value == null) {
            throw new NullPointerException("Parameter cannot be null"); // Spec requirement.
        }
        SomeClass result = new SomeClass();
        result.setFieldName(value + "-Converter2");
        return result;
    }
}
