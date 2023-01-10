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
package be.atbash.runtime.jakarta.executable.impl.mp.config;

import be.atbash.runtime.core.data.deployment.AbstractDeployment;
import be.atbash.runtime.core.data.deployment.ApplicationExecution;
import be.atbash.runtime.core.data.deployment.CurrentDeployment;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class ApplicationDataConfigSourceTest {

    @Test
    void getPropertyNames() {

        AbstractDeployment deployment = new ApplicationExecution(Collections.emptyList(), "/");
        deployment.addDeploymentData("mp-config.prop1", "Ignored");
        deployment.addDeploymentData("jersey.prop2", "Ignored");
        deployment.addDeploymentData("foo", "bar");
        CurrentDeployment.getInstance().setCurrent(deployment);

        ApplicationDataConfigSource configSource = new ApplicationDataConfigSource();
        Assertions.assertThat(configSource.getPropertyNames()).containsExactly("foo");
    }

    @Test
    void getValue() {

        AbstractDeployment deployment = new ApplicationExecution(Collections.emptyList(), "/");
        deployment.addDeploymentData("foo", "bar");
        CurrentDeployment.getInstance().setCurrent(deployment);

        ApplicationDataConfigSource configSource = new ApplicationDataConfigSource();
        Assertions.assertThat(configSource.getValue("foo")).isEqualTo("bar");
    }

    @Test
    void getValue_nonExisting() {

        AbstractDeployment deployment = new ApplicationExecution(Collections.emptyList(), "/");
        deployment.addDeploymentData("foo", "bar");
        CurrentDeployment.getInstance().setCurrent(deployment);

        ApplicationDataConfigSource configSource = new ApplicationDataConfigSource();
        Assertions.assertThat(configSource.getValue("xyz")).isNull();
    }
}