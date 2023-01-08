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
package be.atbash.runtime.core.deployment.monitor;

import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.management.openmbean.CompositeData;
import java.io.File;
import java.util.Collections;

class ApplicationMonTest {

    @Test
    void getRunningApplications() {
        ApplicationMon applicationMon = new ApplicationMon();
        CompositeData[] applications = applicationMon.getRunningApplications();
        Assertions.assertThat(applications).hasSize(0);
    }

    @Test
    void getRunningApplications_single() {
        ApplicationMon applicationMon = new ApplicationMon();

        registerApplication(applicationMon);

        CompositeData[] applications = applicationMon.getRunningApplications();
        Assertions.assertThat(applications).hasSize(1);
        Assertions.assertThat(applications[0].getCompositeType().keySet())
                .containsExactly("ContextRoot", "Name", "Specifications");
        CompositeData data = applications[0];
        Assertions.assertThat(data.get("ContextRoot")).isEqualTo("/test");
        Assertions.assertThat(data.get("Name")).isEqualTo("test");
        Assertions.assertThat(data.get("Specifications")).isEqualTo("REST");
    }

    private static void registerApplication(ApplicationMon applicationMon) {
        ArchiveDeployment deployment = new ArchiveDeployment(new File("./applications/test.war"));

        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .build();

        File targetLocation = new File(runtimeConfiguration.getApplicationDirectory(), deployment.getArchiveFile().getName());
        deployment.setDeploymentLocation(targetLocation);
        deployment.setSpecifications(Collections.singleton(Specification.REST));
        deployment.setSniffers(Collections.emptyList());

        applicationMon.registerApplication(deployment);
    }
}