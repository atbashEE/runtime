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

import be.atbash.runtime.jakarta.executable.testclasses.TestApplication;
import be.atbash.runtime.jakarta.executable.testclasses.TestRunner;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

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
}