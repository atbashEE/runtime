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
package be.atbash.runtime.config.module;

import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static be.atbash.runtime.config.RuntimeConfigConstants.APPLICATIONS_FILE;
import static org.assertj.core.api.Assertions.assertThat;

class ArchiveDeploymentStorageTest {

    @Test
    public void testArchiveDeploymentDone() throws IOException {
        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .build();
        ArchiveDeploymentStorage archiveDeploymentStorage = new ArchiveDeploymentStorage(runtimeConfiguration);

        ArchiveDeployment deployment = new ArchiveDeployment(new File("./applications/test.war"));
        File targetLocation = new File(runtimeConfiguration.getApplicationDirectory(), deployment.getArchiveFile().getName());
        deployment.setDeploymentLocation(targetLocation);
        deployment.setSpecifications(newSet(Specification.REST, Specification.HTML));
        deployment.setSniffers(Collections.singletonList(new TestSniffer1()));
        archiveDeploymentStorage.archiveDeploymentDone(deployment);

        String content = Files.readString(new File(configDirectory, APPLICATIONS_FILE).toPath());

        assertThat(content).contains("\"deploymentName\":\"test\"");
        assertThat(content).contains("\"deploymentLocation\":\"\\/test.war\"");
        assertThat(content).contains("\"specifications\":[\"REST\",\"HTML\"]");
        assertThat(content).contains("\"sniffers\":[\"TestSniffer1\"]");
        assertThat(content).contains("\"contextRoot\":\"\\/test\"");
    }

    private Set<Specification> newSet(Specification... specifications) {
        Set<Specification> result = new HashSet<>();
        Collections.addAll(result, specifications);
        return result;
    }

    @Test
    public void testArchiveDeploymentDone_append() throws IOException {
        File configDirectory = new File("./target/testDirectory2");
        configDirectory.mkdirs();

        String originalContent = "{\"deployments\":[{\"deploymentName\":\"test\",\"deploymentLocation\":\"/test.war\",\"specifications\":[\"REST\",\"HTML\"],\"sniffers\":[\"TestSniffer1\"]}]}";
        Files.writeString(new File(configDirectory, APPLICATIONS_FILE).toPath(), originalContent);

        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .build();
        ArchiveDeploymentStorage archiveDeploymentStorage = new ArchiveDeploymentStorage(runtimeConfiguration);

        ArchiveDeployment deployment = new ArchiveDeployment(new File("./applications/test2.war"));
        File targetLocation = new File(runtimeConfiguration.getApplicationDirectory(), deployment.getArchiveFile().getName());
        deployment.setDeploymentLocation(targetLocation);
        deployment.setSpecifications(newSet(Specification.SERVLET));
        deployment.setSniffers(Collections.singletonList(new TestSniffer2()));
        archiveDeploymentStorage.archiveDeploymentDone(deployment);

        String content = Files.readString(new File(configDirectory, APPLICATIONS_FILE).toPath());

        assertThat(content).contains("{\"deploymentName\":\"test\",\"deploymentLocation\":\"\\/test.war\",\"specifications\":[\"REST\",\"HTML\"],\"sniffers\":[\"TestSniffer1\"]}");
        assertThat(content).contains("\"deploymentName\":\"test2\"");
        assertThat(content).contains("\"deploymentLocation\":\"\\/test2.war\"");
        assertThat(content).contains("\"specifications\":[\"SERVLET\"]");
        assertThat(content).contains("\"sniffers\":[\"TestSniffer2\"]");
    }

    @Test
    public void testArchiveDeploymentDone_doNotOverwrite() throws IOException {
        File configDirectory = new File("./target/testDirectory3");
        configDirectory.mkdirs();

        String originalContent = "{\"deployments\":[{\"deploymentName\":\"test\",\"deploymentLocation\":\"/applications/test.war\",\"specifications\":[\"REST\",\"HTML\"],\"sniffers\":[\"TestSniffer1\"]}]}";
        Files.writeString(new File(configDirectory, APPLICATIONS_FILE).toPath(), originalContent);

        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .build();
        ArchiveDeploymentStorage archiveDeploymentStorage = new ArchiveDeploymentStorage(runtimeConfiguration);

        ArchiveDeployment deployment = new ArchiveDeployment(new File("./applications/test.war"));
        File targetLocation = new File(runtimeConfiguration.getApplicationDirectory(), deployment.getArchiveFile().getName());
        deployment.setDeploymentLocation(targetLocation);
        deployment.setSpecifications(newSet(Specification.SERVLET));
        deployment.setSniffers(Collections.singletonList(new TestSniffer2()));
        archiveDeploymentStorage.archiveDeploymentDone(deployment);

        String content = Files.readString(new File(configDirectory, APPLICATIONS_FILE).toPath());

        assertThat(content).contains("{\"deploymentName\":\"test\",\"deploymentLocation\":\"\\/applications\\/test.war\",\"specifications\":[\"REST\",\"HTML\"],\"sniffers\":[\"TestSniffer1\"]}");
        assertThat(content).doesNotContain("\"deploymentName\":\"test2\"");
        assertThat(content).doesNotContain("\"deploymentLocation\":\"\\/applications\\/test2.war\"");
        assertThat(content).doesNotContain("\"specifications\":[\"SERVLET\"]");
        assertThat(content).doesNotContain("\"sniffers\":[\"TestSniffer2\"]");
    }

    @Test
    public void testArchiveDeploymentRemove() throws IOException {
        File configDirectory = new File("./target/testDirectory4");
        configDirectory.mkdirs();

        String originalContent = "{\"deployments\":[{\"deploymentName\":\"test\",\"deploymentLocation\":\"/test.war\",\"specifications\":[\"REST\",\"HTML\"],\"sniffers\":[\"TestSniffer1\"],\"contextRoot\":\"/test\"}]}";
        Files.writeString(new File(configDirectory, APPLICATIONS_FILE).toPath(), originalContent);

        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .build();
        ArchiveDeploymentStorage archiveDeploymentStorage = new ArchiveDeploymentStorage(runtimeConfiguration);

        ArchiveDeployment deployment = new ArchiveDeployment(new File("./applications/test.war"));
        File targetLocation = new File(runtimeConfiguration.getApplicationDirectory(), deployment.getArchiveFile().getName());
        deployment.setDeploymentLocation(targetLocation);
        deployment.setSpecifications(newSet(Specification.SERVLET));
        deployment.setSniffers(Collections.singletonList(new TestSniffer2()));
        deployment.setContextRoot("/test");  // Important for test, internally identity is determined based on contextroot

        archiveDeploymentStorage.archiveDeploymentRemoved(deployment);

        String content = Files.readString(new File(configDirectory, APPLICATIONS_FILE).toPath());

        assertThat(content).contains("{\"deployments\":[]}");
    }
}