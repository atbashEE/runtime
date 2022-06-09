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
package be.atbash.runtime.core.deployment.data;

import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

class DeploymentDataFromCommandTest {

    @Test
    void getDeploymentData() {
        DeploymentDataFromCommand retriever = new DeploymentDataFromCommand();

        ArchiveDeployment deployment = new ArchiveDeployment(new File("./applications/test.war"));
        File file = new File("./src/test/resources/DeploymentData.properties");

        // To see if location is correct
        Assertions.assertThat(file.exists()).isTrue();
        deployment.setConfigDataFile(file);

        Map<String, String> data = retriever.getDeploymentData(deployment);
        Assertions.assertThat(data.keySet()).containsOnly("key", "mp-config.enabled");
        Assertions.assertThat(data.get("key")).isEqualTo("value");
        Assertions.assertThat(data.get("mp-config.enabled")).isEqualTo("true");

    }

    @Test
    void getDeploymentData_noFile() {
        DeploymentDataFromCommand retriever = new DeploymentDataFromCommand();

        ArchiveDeployment deployment = new ArchiveDeployment(new File("./applications/test.war"));

        Map<String, String> data = retriever.getDeploymentData(deployment);
        Assertions.assertThat(data).isEmpty();

    }

}