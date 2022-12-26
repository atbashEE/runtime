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
package be.atbash.runtime.jakarta.executable;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class JakartaRunnerDataTest {

    @Test
    void getResources() {
        JakartaRunnerData runnerData = new JakartaRunnerData();
        runnerData.addResources(String.class, JakartaRunner.class);

        Assertions.assertThat(runnerData.getResources()).containsOnly(String.class, JakartaRunner.class);
    }

    @Test
    void getPort_defaultValue() {
        JakartaRunnerData runnerData = new JakartaRunnerData();
        Assertions.assertThat(runnerData.getPort()).isEqualTo(8080);
    }

    @Test
    void setPort() {
        JakartaRunnerData runnerData = new JakartaRunnerData();
        runnerData.setPort(8888);
        Assertions.assertThat(runnerData.getPort()).isEqualTo(8888);
    }

    @Test
    void getHost_defaultValue() {
        JakartaRunnerData runnerData = new JakartaRunnerData();
        Assertions.assertThat(runnerData.getHost()).isEqualTo("localhost");
    }

    @Test
    void setHost() {
        JakartaRunnerData runnerData = new JakartaRunnerData();
        runnerData.setHost("my-server");
        Assertions.assertThat(runnerData.getHost()).isEqualTo("my-server");
    }

    @Test
    void setRoot_default() {
        JakartaRunnerData runnerData = new JakartaRunnerData();
        Assertions.assertThat(runnerData.getRoot()).isEqualTo("/");
    }

    @Test
    void setRoot_correct() {
        JakartaRunnerData runnerData = new JakartaRunnerData();
        runnerData.setRoot("/root");
        Assertions.assertThat(runnerData.getRoot()).isEqualTo("/root");
    }

    @Test
    void setRoot_missingLeading() {
        JakartaRunnerData runnerData = new JakartaRunnerData();
        runnerData.setRoot("root");
        Assertions.assertThat(runnerData.getRoot()).isEqualTo("/root");
    }

    @Test
    void setRoot_extraSlash() {
        JakartaRunnerData runnerData = new JakartaRunnerData();
        runnerData.setRoot("/root/");
        Assertions.assertThat(runnerData.getRoot()).isEqualTo("/root");
    }

    @Test
    void setRoot_noNull() {
        JakartaRunnerData runnerData = new JakartaRunnerData();
        Assertions.assertThatThrownBy(() -> runnerData.setRoot(null))
                .isInstanceOf(NullPointerException.class);

    }
}