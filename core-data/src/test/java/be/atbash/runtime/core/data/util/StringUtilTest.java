/*
 * Copyright 2021-2023 Rudy De Busscher (https://www.atbash.be)
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

import org.junit.jupiter.api.Test;

import org.assertj.core.api.Assertions;

class StringUtilTest {

    @Test
    void determineDeploymentName() {
        Assertions.assertThat(StringUtil.determineDeploymentName("test.war")).isEqualTo("test");
    }

    @Test
    void determineDeploymentNameMultipleDots() {
        Assertions.assertThat(StringUtil.determineDeploymentName("my.test.archive.war")).isEqualTo("my.test.archive");
    }

    @Test
    void sanitizePath() {
        Assertions.assertThat(StringUtil.sanitizePath("/root")).isEqualTo("/root");
    }

    @Test
    void sanitizePath_missingSlash() {
        Assertions.assertThat(StringUtil.sanitizePath("root")).isEqualTo("/root");
    }

    @Test
    void sanitizePath_ExtraSlash() {
        Assertions.assertThat(StringUtil.sanitizePath("/root/")).isEqualTo("/root");
    }

    @Test
    void sanitizePath_trim() {
        Assertions.assertThat(StringUtil.sanitizePath("   /ro ot   ")).isEqualTo("/ro ot");
    }

    @Test
    void sanitizePath_empty() {
        Assertions.assertThat(StringUtil.sanitizePath("")).isEqualTo("/");
    }

    @Test
    void sanitizePath_null() {
        Assertions.assertThat(StringUtil.sanitizePath(null)).isNull();
    }
}