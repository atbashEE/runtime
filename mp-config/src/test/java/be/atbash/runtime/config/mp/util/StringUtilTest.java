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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class StringUtilTest {

    @Test
    void split() {
        String text = "large:cheese\\,mushroom,medium:chicken,small:pepperoni";

        String[] split = StringUtil.split(text);
        Assertions.assertThat(split).hasSize(3);
        Assertions.assertThat(split[0]).isEqualTo("large:cheese,mushroom");
        Assertions.assertThat(split[1]).isEqualTo("medium:chicken");
        Assertions.assertThat(split[2]).isEqualTo("small:pepperoni");
    }

    @Test
    void trailingSegmentsIgnored() {
        String text = "foo,bar,baz,,,,,";
        String[] split = StringUtil.split(text);
        Assertions.assertThat(split).hasSize(3);
        Assertions.assertThat(split[0]).isEqualTo("foo");
        Assertions.assertThat(split[1]).isEqualTo("bar");
        Assertions.assertThat(split[2]).isEqualTo("baz");

    }

    @Test
    void leadingSegmentsIgnored() {
        String text = ",,,,,,,,foo,bar,baz";
        String[] split = StringUtil.split(text);
        Assertions.assertThat(split).hasSize(3);
        Assertions.assertThat(split[0]).isEqualTo("foo");
        Assertions.assertThat(split[1]).isEqualTo("bar");
        Assertions.assertThat(split[2]).isEqualTo("baz");

    }

    @Test
    void midSegmentsIgnored() {
        String text = "foo,,,,bar,,,baz";
        String[] split = StringUtil.split(text);
        Assertions.assertThat(split).hasSize(3);
        Assertions.assertThat(split[0]).isEqualTo("foo");
        Assertions.assertThat(split[1]).isEqualTo("bar");
        Assertions.assertThat(split[2]).isEqualTo("baz");

    }

    @Test
    void allEmptySegments() {
        String text = ",,,,,";
        String[] split = StringUtil.split(text);
        Assertions.assertThat(split).hasSize(0);
    }

    @Test
    void twoEmptySegments() {
        String text = ",";
        String[] split = StringUtil.split(text);
        Assertions.assertThat(split).hasSize(0);
    }

    @Test
    void emptyString() {
        String text = "";
        String[] split = StringUtil.split(text);
        Assertions.assertThat(split).hasSize(0);

    }

    @Test
    void escapingSituations() {
        String text = "foo\\\\,bar\\x,,,baz";
        String[] split = StringUtil.split(text);

        Assertions.assertThat(split[0]).isEqualTo("foo\\");
        Assertions.assertThat(split[1]).isEqualTo("barx");
        Assertions.assertThat(split[2]).isEqualTo("baz");

    }

    @Test
    void replaceNonAlphanumericByUnderscores() {

        String converted = StringUtil.replaceNonAlphanumericByUnderscores("hng+KZhyGB3N=!G9aMk$");
        Assertions.assertThat(converted).isEqualTo("hng_KZhyGB3N__G9aMk_");
    }

}