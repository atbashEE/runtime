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
package be.atbash.runtime.core.data.util;

import be.atbash.util.TestReflectionUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

class SystemPropertyUtilTest {

    @AfterEach
    public void cleanUp() throws NoSuchFieldException {
        System.clearProperty("atbash.runtime.tck");
        System.clearProperty("atbash.runtime.tck.jwt");
        // reset the JVM Singleton between each run !
        Map<?,?> cache = TestReflectionUtils.getValueOf(SystemPropertyUtil.getInstance(), "resultCache");
        cache.clear();
    }

    @Test
    void isTck_noPropertySpecified() {
        Assertions.assertThat(SystemPropertyUtil.getInstance().isTck("jwt")).isFalse();
    }

    @Test
    void isTck_propertySpecifiedForModule() {
        System.setProperty("atbash.runtime.tck.jwt", "true");
        Assertions.assertThat(SystemPropertyUtil.getInstance().isTck("jwt")).isTrue();
    }

    @Test
    void isTck_propertySpecifiedOverall() {
        System.setProperty("atbash.runtime.tck", "true");
        Assertions.assertThat(SystemPropertyUtil.getInstance().isTck("jwt")).isTrue();
    }

    @Test
    void isTck_propertySpecifiedModuleIsPreferred() {
        System.setProperty("atbash.runtime.tck", "true");
        System.setProperty("atbash.runtime.tck.jwt", "false");
        Assertions.assertThat(SystemPropertyUtil.getInstance().isTck("jwt")).isFalse();
    }
}