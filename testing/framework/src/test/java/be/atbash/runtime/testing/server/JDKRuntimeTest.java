/*
 * Copyright 2021 Rudy De Busscher (https://www.atbash.be)
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
package be.atbash.runtime.testing.server;

import be.atbash.runtime.testing.model.JDKRuntime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JDKRuntimeTest {


    @Test
    void jdk11Value() {
        JDKRuntime runtime = JDKRuntime.parse("jdk11");
        assertThat(runtime).isEqualTo(JDKRuntime.JDK11);

        runtime = JDKRuntime.parse("JDK11");
        assertThat(runtime).isEqualTo(JDKRuntime.JDK11);

        runtime = JDKRuntime.parse("jDk11");
        assertThat(runtime).isEqualTo(JDKRuntime.JDK11);
    }

    @Test
    void jdk17Value() {
        JDKRuntime runtime = JDKRuntime.parse("jdk17");
        assertThat(runtime).isEqualTo(JDKRuntime.JDK17);

        runtime = JDKRuntime.parse("JDK17");
        assertThat(runtime).isEqualTo(JDKRuntime.JDK17);

        runtime = JDKRuntime.parse("jDk17");
        assertThat(runtime).isEqualTo(JDKRuntime.JDK17);
    }

    @Test
    void jdk18Value() {
        JDKRuntime runtime = JDKRuntime.parse("jdk18");
        assertThat(runtime).isEqualTo(JDKRuntime.JDK18);

        runtime = JDKRuntime.parse("JDK18");
        assertThat(runtime).isEqualTo(JDKRuntime.JDK18);

        runtime = JDKRuntime.parse("jDk18");
        assertThat(runtime).isEqualTo(JDKRuntime.JDK18);
    }

    @Test
    void jdk19Value() {
        JDKRuntime runtime = JDKRuntime.parse("jdk19");
        assertThat(runtime).isEqualTo(JDKRuntime.JDK19);

        runtime = JDKRuntime.parse("JDK19");
        assertThat(runtime).isEqualTo(JDKRuntime.JDK19);

        runtime = JDKRuntime.parse("jDk19");
        assertThat(runtime).isEqualTo(JDKRuntime.JDK19);
    }

    @Test
    void otherValue() {
        JDKRuntime runtime = JDKRuntime.parse("");
        assertThat(runtime).isEqualTo(JDKRuntime.UNKNOWN);

        runtime = JDKRuntime.parse(" ");
        assertThat(runtime).isEqualTo(JDKRuntime.UNKNOWN);

        runtime = JDKRuntime.parse("anything");
        assertThat(runtime).isEqualTo(JDKRuntime.UNKNOWN);
    }
}