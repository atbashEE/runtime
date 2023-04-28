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
package be.atbash.runtime.jersey.util;

import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.deployment.DeploymentDataConstants;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

class ExtraPackagesUtilTest {

    @Test
    void addPackages() {

        ArchiveDeployment deployment = createArchiveDeployment();

        ExtraPackagesUtil.addPackages(deployment, "be.atbash");

        String names = deployment.getDeploymentData(DeploymentDataConstants.EXTRA_PACKAGE_NAMES);
        Assertions.assertThat(names).isEqualTo("be.atbash");
    }

    @Test
    void addPackages_multiple() {

        ArchiveDeployment deployment = createArchiveDeployment();

        ExtraPackagesUtil.addPackages(deployment, "be.atbash.runtime.cdi", "be.atbash.runtime.jaxrs");

        String names = deployment.getDeploymentData(DeploymentDataConstants.EXTRA_PACKAGE_NAMES);
        Assertions.assertThat(names).isEqualTo("be.atbash.runtime.cdi;be.atbash.runtime.jaxrs");
    }

    @Test
    void addPackages_add() {

        ArchiveDeployment deployment = createArchiveDeployment();
        deployment.addDeploymentData(DeploymentDataConstants.EXTRA_PACKAGE_NAMES, "be.atbash.runtime.cdi");
        ExtraPackagesUtil.addPackages(deployment, "be.atbash.runtime.jaxrs");

        String names = deployment.getDeploymentData(DeploymentDataConstants.EXTRA_PACKAGE_NAMES);
        Assertions.assertThat(names).isEqualTo("be.atbash.runtime.cdi;be.atbash.runtime.jaxrs");
    }

    @Test
    void addPackages_list() {

        ArchiveDeployment deployment = createArchiveDeployment();

        ExtraPackagesUtil.addPackages(deployment, List.of("be.atbash.runtime.jaxrs"));

        String names = deployment.getDeploymentData(DeploymentDataConstants.EXTRA_PACKAGE_NAMES);
        Assertions.assertThat(names).isEqualTo("be.atbash.runtime.jaxrs");
    }

    @Test
    void addPackages_list_add() {

        ArchiveDeployment deployment = createArchiveDeployment();
        deployment.addDeploymentData(DeploymentDataConstants.EXTRA_PACKAGE_NAMES, "be.atbash.runtime.cdi");
        ExtraPackagesUtil.addPackages(deployment, List.of("be.atbash.runtime.jaxrs"));

        String names = deployment.getDeploymentData(DeploymentDataConstants.EXTRA_PACKAGE_NAMES);
        Assertions.assertThat(names).isEqualTo("be.atbash.runtime.cdi;be.atbash.runtime.jaxrs");
    }

    private static ArchiveDeployment createArchiveDeployment() {
        String strTmp = System.getProperty("java.io.tmpdir");
        File archive = new File(strTmp, "junit.war");
        return new ArchiveDeployment(archive, "customName");
    }
}