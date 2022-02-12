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
package be.atbash.runtime.core.data.config;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class LoggingTest {

    private Logging logging;

    @BeforeEach
    public void setup() {
        logging = new Logging();
    }

    @Test
    void isLogToConsole() {
        logging.setLogToConsole(true);
        Assertions.assertThat(logging.isLogToConsole()).isTrue();

        logging.setLogToConsole(false);
        Assertions.assertThat(logging.isLogToConsole()).isFalse();
    }

    @Test
    void isLogToConsole_Overrule() {
        logging.setLogToConsole(false);
        Assertions.assertThat(logging.isLogToConsole()).isFalse();

        logging.overruleLogToConsole(true);
        Assertions.assertThat(logging.isLogToConsole()).isTrue();


    }

    @Test
    void isLogToFile() {
        logging.setLogToFile(true);
        Assertions.assertThat(logging.isLogToFile()).isTrue();

        logging.setLogToFile(false);
        Assertions.assertThat(logging.isLogToFile()).isFalse();
    }

    @Test
    void isLogToFile_Overrule() {
        logging.setLogToFile(false);
        Assertions.assertThat(logging.isLogToFile()).isFalse();

        logging.overruleLogToFile(true);
        Assertions.assertThat(logging.isLogToFile()).isTrue();


    }
}