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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static be.atbash.util.resource.ResourceUtil.CLASSPATH_PREFIX;

class ResourceReaderTest {

    @Test
    void readResource() throws IOException {
        String text = ResourceReader.readResource(CLASSPATH_PREFIX + "test.txt");
        Assertions.assertThat(text).isEqualTo("The content of the resource");
    }

    @Test
    void existsResource() {
        Assertions.assertThat(ResourceReader.existsResource(CLASSPATH_PREFIX + "test.txt")).isTrue();
    }

    @Test
    void existsResource_nonExisting() {
        Assertions.assertThat(ResourceReader.existsResource(CLASSPATH_PREFIX + "someRandomFile.txt")).isFalse();
    }
}