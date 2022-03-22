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
package be.atbash.runtime.logging.handler.formatter;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

class AnsiColorTest {

    @Test
    void parse() {

        Optional<AnsiColor> black = AnsiColor.parse("BLACK");
        Assertions.assertThat(black).hasValue(AnsiColor.BLACK);
    }

    @Test
    void parse_lowercase() {

        Optional<AnsiColor> black = AnsiColor.parse("red");
        Assertions.assertThat(black).hasValue(AnsiColor.RED);
    }

    @Test
    void parse_notfound() {

        Optional<AnsiColor> black = AnsiColor.parse("test");
        Assertions.assertThat(black).isEmpty();
    }

    @Test
    void parse_null() {

        Optional<AnsiColor> black = AnsiColor.parse(null);
        Assertions.assertThat(black).isEmpty();
    }
}