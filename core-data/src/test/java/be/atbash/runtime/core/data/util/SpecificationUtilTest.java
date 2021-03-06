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
package be.atbash.runtime.core.data.util;

import be.atbash.runtime.core.data.Specification;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class SpecificationUtilTest {

    @Test
    public void testAsEnum() {
        Set<String> values = new HashSet<>(Arrays.asList("HtMl", "SERVLET", "Something"));
        Set<Specification> specifications = SpecificationUtil.asEnum(values);

        assertThat(specifications).contains(Specification.HTML, Specification.SERVLET);
    }

    @Test
    public void testAsEnum_empty() {
        Set<String> values = new HashSet<>();
        Set<Specification> specifications = SpecificationUtil.asEnum(values);

        assertThat(specifications).isEmpty();
    }

}