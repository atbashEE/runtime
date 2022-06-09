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
import java.util.Map;
import java.util.Set;

class DeploymentMetadataTest {

    @Test
    void testEquals() {
        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .build();

        List<Sniffer> sniffers = Collections.singletonList(new TestSniffer());
        Set<Specification> specifications = Collections.singleton(Specification.SERVLET);


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
        Set<Specification> specifications = Collections.singleton(Specification.SERVLET);


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

    @Test
    void testDeploymentLocation() {
        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .build();

        List<Sniffer> sniffers = Collections.singletonList(new TestSniffer());
        Set<Specification> specifications = Collections.singleton(Specification.SERVLET);


        ArchiveDeployment deployment = new ArchiveDeployment(new File("./applications/test1.war"));
        File targetLocation1 = new File(runtimeConfiguration.getApplicationDirectory(), deployment.getArchiveFile().getName());
        deployment.setDeploymentLocation(targetLocation1);
        deployment.setSniffers(sniffers);
        deployment.setSpecifications(specifications);
        deployment.setContextRoot("/root1");


        DeploymentMetadata metadata = new DeploymentMetadata(deployment, runtimeConfiguration);
        Assertions.assertThat(metadata.getDeploymentLocation()).isEqualTo("/test1.war");
    }


    @Test
    void testConfigDataFile() {
        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .build();

        List<Sniffer> sniffers = Collections.singletonList(new TestSniffer());
        Set<Specification> specifications = Collections.singleton(Specification.SERVLET);


        ArchiveDeployment deployment = new ArchiveDeployment(new File("./applications/test1.war"));
        File targetLocation1 = new File(runtimeConfiguration.getApplicationDirectory(), deployment.getArchiveFile().getName());
        deployment.setDeploymentLocation(targetLocation1);
        deployment.setSniffers(sniffers);
        deployment.setSpecifications(specifications);
        deployment.setContextRoot("/root1");

        File file = new File("./src/test/resources/DeploymentData.properties");
        deployment.setConfigDataFile(file);

        DeploymentMetadata metadata = new DeploymentMetadata(deployment, runtimeConfiguration);
        String location = "/src/test/resources/DeploymentData.properties";
        Assertions.assertThat(metadata.getConfigDataFile()).endsWith(location);

        // Larger than location as we requested full path.
        Assertions.assertThat(metadata.getConfigDataFile().length()).isGreaterThan(location.length() + 10);

    }

    @Test
    void testDeploymentLocation_corrupted() {
        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .build();

        List<Sniffer> sniffers = Collections.singletonList(new TestSniffer());
        Set<Specification> specifications = Collections.singleton(Specification.SERVLET);


        ArchiveDeployment deployment = new ArchiveDeployment(new File("./applications/test1.war"));
        deployment.setSniffers(sniffers);
        deployment.setSpecifications(specifications);
        deployment.setContextRoot("/root1");


        DeploymentMetadata metadata = new DeploymentMetadata(deployment, runtimeConfiguration);
        Assertions.assertThat(metadata.getDeploymentLocation()).isNull();
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

        @Override
        public Map<String, String> deploymentData() {
            return Collections.EMPTY_MAP;
        }

    }
}