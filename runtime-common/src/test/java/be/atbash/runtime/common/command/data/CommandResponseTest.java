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
package be.atbash.runtime.common.command.data;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class CommandResponseTest {

    @Test
    void isSuccess() {
        CommandResponse response = new CommandResponse();
        // By default true
        Assertions.assertThat(response.isSuccess()).isTrue();
    }

    @Test
    void setErrorMessage() {
        CommandResponse response = new CommandResponse();
        response.setErrorMessage("The error");
        Assertions.assertThat(response.isSuccess()).isFalse();
        Assertions.assertThat(response.getErrorMessage()).isEqualTo("The error");
    }

    @Test
    void setErrorMessage_withNull() {
        CommandResponse response = new CommandResponse();
        // This happens in the serialization from JSON to Java.
        // We don't want to have a result false in this case.
        response.setErrorMessage(null);

        Assertions.assertThat(response.isSuccess()).isTrue();
        Assertions.assertThat(response.getErrorMessage()).isNull();
    }
}