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
package be.atbash.runtime.core.data.deployment.info;

import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;

class DeploymentMetadataTest {

    @Test
    void testEquals() {
        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .build();

        List<Sniffer> sniffers = Collections.singletonList(new TestSniffer());
        List<Specification> specifications = Collections.singletonList(Specification.SERVLET);


        ArchiveDeployment deployment1 = new ArchiveDeployment(new File("./applications/test1.war"));
        File targetLocation1 = new File(runtimeConfiguration.getApplicationDirectory(), deployment1.getArchiveFile().getName());
        deployment1.setDeploymentLocation(targetLocation1);
        deployment1.setSniffers(sniffers);
        deployment1.setSpecifications(specifications);
        deployment1.setContextRoot("/root1");

        ArchiveDeployment deployment2 = new ArchiveDeployment(new File("./applications/test2.war"));
        File targetLocation2 = new File(runtimeConfiguration.getApplicationDirectory(), deployment1.getArchiveFile().getName());
        deployment2.setDeploymentLocation(targetLocation2);
        deployment2.setSniffers(sniffers);
        deployment2.setSpecifications(specifications);
        deployment2.setContextRoot("/root1");


        Assertions.assertThat(new DeploymentMetadata(deployment1, runtimeConfiguration))
                .isEqualTo(new DeploymentMetadata(deployment2, runtimeConfiguration));


    }

    @Test
    void testEquals_different() {
        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .build();

        List<Sniffer> sniffers = Collections.singletonList(new TestSniffer());
        List<Specification> specifications = Collections.singletonList(Specification.SERVLET);


        ArchiveDeployment deployment1 = new ArchiveDeployment(new File("./applications/test1.war"));
        File targetLocation1 = new File(runtimeConfiguration.getApplicationDirectory(), deployment1.getArchiveFile().getName());
        deployment1.setDeploymentLocation(targetLocation1);
        deployment1.setSniffers(sniffers);
        deployment1.setSpecifications(specifications);
        deployment1.setContextRoot("/root1");

        ArchiveDeployment deployment2 = new ArchiveDeployment(new File("./applications/test2.war"));
        File targetLocation2 = new File(runtimeConfiguration.getApplicationDirectory(), deployment1.getArchiveFile().getName());
        deployment2.setDeploymentLocation(targetLocation2);
        deployment2.setSniffers(sniffers);
        deployment2.setSpecifications(specifications);
        deployment2.setContextRoot("/root2");


        Assertions.assertThat(new DeploymentMetadata(deployment1, runtimeConfiguration))
                .isNotEqualTo(new DeploymentMetadata(deployment2, runtimeConfiguration));


    }

    private static class TestSniffer implements Sniffer {

        @Override
        public Specification[] detectedSpecifications() {
            return new Specification[0];
        }

        @Override
        public boolean triggered(Class<?> aClass) {
            return false;
        }

        @Override
        public boolean triggered(String descriptorName, String content) {
            return false;
        }

        @Override
        public boolean isFastDetection() {
            return false;
        }
    }
}