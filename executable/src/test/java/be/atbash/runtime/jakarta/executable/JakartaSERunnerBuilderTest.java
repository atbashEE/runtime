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
package be.atbash.runtime.jakarta.executable;

import be.atbash.runtime.jakarta.executable.testclasses.TestApplication;
import be.atbash.runtime.jakarta.executable.testclasses.TestRunner;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class JakartaSERunnerBuilderTest {

    @Test
    void newBuilder_withApplication() {
        JakartaSERunnerBuilder.newBuilder(TestApplication.class)
                .withPort(8888)
                .run();

        Assertions.assertThat(TestRunner.jakartaRunnerData).isNotNull();
        Assertions.assertThat(TestRunner.jakartaRunnerData.getPort()).isEqualTo(8888);
        Assertions.assertThat(TestRunner.jakartaRunnerData.getResources()).containsOnly(TestApplication.class);
    }

    @Test
    void builder_wrongPort() {
        Assertions.assertThatThrownBy(
                        () -> JakartaSERunnerBuilder.newBuilder(TestApplication.class)
                                .withPort(-123))
                .isInstanceOf(ParameterValidationException.class)
                .hasMessage("The port value must be between 0 and 65536");
    }

    @Test
    void builder_wrongPort2() {
        Assertions.assertThatThrownBy(
                        () -> JakartaSERunnerBuilder.newBuilder(TestApplication.class)
                                .withPort(100_000))
                .isInstanceOf(ParameterValidationException.class)
                .hasMessage("The port value must be between 0 and 65536");
    }

    @Test
    void builder_wrongHost() {
        Assertions.assertThatThrownBy(
                        () -> JakartaSERunnerBuilder.newBuilder(TestApplication.class)
                                .withHost("hola"))
                .isInstanceOf(ParameterValidationException.class)
                .hasMessage("The host does not resolve or address is not a local address.");
    }

    @Test
    void builder_wrongHost_notLocal() {
        // Resolvable address but not local
        Assertions.assertThatThrownBy(
                        () -> JakartaSERunnerBuilder.newBuilder(TestApplication.class)
                                .withHost("atbash.be"))
                .isInstanceOf(ParameterValidationException.class)
                .hasMessage("The host does not resolve or address is not a local address.");
    }

    @Test
    void builder_correctHost() {
        // localhost should always resolve
        Assertions.assertThatCode(() ->
                JakartaSERunnerBuilder.newBuilder(TestApplication.class)
                        .withHost("127.0.0.1")

        ).doesNotThrowAnyException();

    }

    @Test
    void builder_addCommandLineEntry_withOption() {
        JakartaSERunnerBuilder.newBuilder(TestApplication.class)
                .addCommandLineEntry("-w JFR").run();

        Assertions.assertThat(TestRunner.jakartaRunnerData).isNotNull();
        Assertions.assertThat(TestRunner.jakartaRunnerData.getCommandLineEntries()).containsExactly("-w", "JFR");
    }

    @Test
    void builder_addCommandLineEntry_single() {
        JakartaSERunnerBuilder.newBuilder(TestApplication.class)
                .addCommandLineEntry("-v").run();

        Assertions.assertThat(TestRunner.jakartaRunnerData).isNotNull();
        Assertions.assertThat(TestRunner.jakartaRunnerData.getCommandLineEntries()).containsExactly("-v");
    }

    @Test
    void builder_addConfig_pair() {
        JakartaSERunnerBuilder.newBuilder(TestApplication.class)
                .addConfig("foo", "bar").run();

        Assertions.assertThat(TestRunner.jakartaRunnerData).isNotNull();
        Assertions.assertThat(TestRunner.jakartaRunnerData.getApplicationData()).contains(Assertions.entry("foo", "bar"));
    }

    @Test
    void builder_addConfig_map() {
        Map<String, String> data = new HashMap<>();
        data.put("key", "value");
        JakartaSERunnerBuilder.newBuilder(TestApplication.class)
                .addConfig(data).run();

        Assertions.assertThat(TestRunner.jakartaRunnerData).isNotNull();
        Assertions.assertThat(TestRunner.jakartaRunnerData.getApplicationData()).contains(Assertions.entry("key", "value"));
    }
}