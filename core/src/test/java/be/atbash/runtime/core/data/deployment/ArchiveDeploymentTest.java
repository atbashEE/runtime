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
package be.atbash.runtime.core.data.deployment;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

class ArchiveDeploymentTest {

    @Test
    public void testDeploymentWithoutName() {
        String strTmp = System.getProperty("java.io.tmpdir");
        File archive = new File(strTmp, "junit.war");
        ArchiveDeployment deployment = new ArchiveDeployment(archive);
        Assertions.assertThat(deployment.isDeployed()).isFalse();
        Assertions.assertThat(deployment.getDeploymentName()).isEqualTo("junit");
    }


    @Test
    public void testDeploymentMutlipleDots() {
        String strTmp = System.getProperty("java.io.tmpdir");
        File archive = new File(strTmp, "my.test.application.war");
        ArchiveDeployment deployment = new ArchiveDeployment(archive);
        Assertions.assertThat(deployment.isDeployed()).isFalse();
        Assertions.assertThat(deployment.getDeploymentName()).isEqualTo("my.test.application");
    }

    @Test
    public void testDeploymentWithName() {
        String strTmp = System.getProperty("java.io.tmpdir");
        File archive = new File(strTmp, "junit.war");
        ArchiveDeployment deployment = new ArchiveDeployment(archive, "customName");
        Assertions.assertThat(deployment.isDeployed()).isTrue();
        Assertions.assertThat(deployment.getDeploymentName()).isEqualTo("customName");
    }

}