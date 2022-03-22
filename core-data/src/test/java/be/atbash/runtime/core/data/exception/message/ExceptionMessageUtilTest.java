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
package be.atbash.runtime.core.data.exception.message;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ExceptionMessageUtilTest {

    @Test
    void formatMessage() {
        ExceptionMessageUtil.addModule("core-data");
        ExceptionMessageUtil.addModule("junit");
        String msg = ExceptionMessageUtil.formatMessage("MODULE-001");
        Assertions.assertThat(msg).isEqualTo("MODULE-001: Abort");
    }

    @Test
    void formatMessage_plainMessage() {
        ExceptionMessageUtil.addModule("core-data");
        ExceptionMessageUtil.addModule("junit");
        String msg = ExceptionMessageUtil.formatMessage("This should not be looked up");
        Assertions.assertThat(msg).isEqualTo("This should not be looked up");
    }

    @Test
    void formatMessage_slf4j_parameters() {
        ExceptionMessageUtil.addModule("core-data");
        ExceptionMessageUtil.addModule("junit");
        String msg = ExceptionMessageUtil.formatMessage("plain message with parameter {}", "Atbash");
        Assertions.assertThat(msg).isEqualTo("plain message with parameter Atbash");
    }

    @Test
    void formatMessage_jul_parameters() {
        ExceptionMessageUtil.addModule("core-data");
        ExceptionMessageUtil.addModule("junit");
        String msg = ExceptionMessageUtil.formatMessage("{0} - plain message with parameter", "Atbash");
        Assertions.assertThat(msg).isEqualTo("Atbash - plain message with parameter");
    }

    @Test
    void formatMessage_withParameter() {
        ExceptionMessageUtil.addModule("core-data");
        ExceptionMessageUtil.addModule("junit");
        String msg = ExceptionMessageUtil.formatMessage("TEST02", "Atbash");
        Assertions.assertThat(msg).isEqualTo("Test02 - Only in main language : parameter = Atbash");
    }
}