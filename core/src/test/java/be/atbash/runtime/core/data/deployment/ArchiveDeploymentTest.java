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
package be.atbash.runtime.core.data.deployment;

import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.WebAppClassLoader;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.core.deployment.sniffer.SingleTriggeredSniffer;
import be.atbash.runtime.core.modules.Module1;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;

class ArchiveDeploymentTest {

    @Test
    public void testDeploymentWithoutName() {
        String strTmp = System.getProperty("java.io.tmpdir");
        File archive = new File(strTmp, "junit.war");
        ArchiveDeployment deployment = new ArchiveDeployment(archive);
        Assertions.assertThat(deployment.getDeploymentPhase().isDeployed()).isFalse();
        Assertions.assertThat(deployment.getDeploymentName()).isEqualTo("junit");
        Assertions.assertThat(deployment.getDeploymentPhase().isVerified()).isTrue();
        Assertions.assertThat(deployment.getDeploymentPhase().isPrepared()).isFalse();
        Assertions.assertThat(deployment.hasDeploymentFailed()).isFalse();
    }

    @Test
    public void testDeploymentMultipleDots() {
        String strTmp = System.getProperty("java.io.tmpdir");
        File archive = new File(strTmp, "my.test.application.war");
        ArchiveDeployment deployment = new ArchiveDeployment(archive);
        Assertions.assertThat(deployment.getDeploymentPhase().isDeployed()).isFalse();
        Assertions.assertThat(deployment.getDeploymentName()).isEqualTo("my.test.application");
    }

    @Test
    public void testDeploymentWithName() {
        String strTmp = System.getProperty("java.io.tmpdir");
        File archive = new File(strTmp, "junit.war");
        ArchiveDeployment deployment = new ArchiveDeployment(archive, "customName");
        Assertions.assertThat(deployment.getDeploymentPhase().isDeployed()).isFalse();
        Assertions.assertThat(deployment.getDeploymentName()).isEqualTo("customName");
        Assertions.assertThat(deployment.getDeploymentPhase().isVerified()).isTrue();
        Assertions.assertThat(deployment.getDeploymentPhase().isPrepared()).isFalse();
        Assertions.assertThat(deployment.hasDeploymentFailed()).isFalse();
    }


    @Test
    public void testDeploymentContextRoot() {
        String strTmp = System.getProperty("java.io.tmpdir");
        File archive = new File(strTmp, "junit.war");
        ArchiveDeployment deployment = new ArchiveDeployment(archive);
        Assertions.assertThat(deployment.getDeploymentPhase().isDeployed()).isFalse();
        Assertions.assertThat(deployment.getDeploymentName()).isEqualTo("junit");
        // Default contextRoot based on deploymentName
        Assertions.assertThat(deployment.getContextRoot()).isEqualTo("/junit");
        Assertions.assertThat(deployment.hasDeploymentFailed()).isFalse();
    }

    @Test
    public void testDeploymentWithLocation() {
        String strTmp = System.getProperty("java.io.tmpdir");
        File location = new File(strTmp, "junit.war");
        List<Sniffer> sniffers = Collections.singletonList(new SingleTriggeredSniffer());
        Set<Specification> specifications = Collections.singleton(Specification.SERVLET);
        ArchiveDeployment deployment = new ArchiveDeployment(location.getAbsolutePath(), "customName", specifications, sniffers, "/junit", new HashMap<>());
        Assertions.assertThat(deployment.getDeploymentPhase().isDeployed()).isFalse();
        Assertions.assertThat(deployment.getDeploymentName()).isEqualTo("customName");
        Assertions.assertThat(deployment.getDeploymentPhase().isVerified()).isFalse();
        Assertions.assertThat(deployment.getDeploymentPhase().isPrepared()).isFalse();
        Assertions.assertThat(deployment.hasDeploymentFailed()).isFalse();
    }

    @Test
    public void testDeploymentVerification() {
        String strTmp = System.getProperty("java.io.tmpdir");
        File location = new File(strTmp, "junit.war");
        List<Sniffer> sniffers = Collections.singletonList(new SingleTriggeredSniffer());
        Set<Specification> specifications = Collections.singleton(Specification.SERVLET);
        ArchiveDeployment deployment = new ArchiveDeployment(location.getAbsolutePath(), "customName", specifications, sniffers, "/junit", new HashMap<>());
        Assertions.assertThat(deployment.getDeploymentPhase().isVerified()).isFalse();
        Assertions.assertThat(deployment.getDeploymentPhase().isPrepared()).isFalse();
        Assertions.assertThat(deployment.getDeploymentPhase().isDeployed()).isFalse();

        deployment.setDeploymentLocation(location);
        Assertions.assertThat(deployment.getDeploymentPhase().isVerified()).isTrue();
        Assertions.assertThat(deployment.getDeploymentPhase().isPrepared()).isFalse();
        Assertions.assertThat(deployment.getDeploymentPhase().isDeployed()).isFalse();
        Assertions.assertThat(deployment.hasDeploymentFailed()).isFalse();
    }

    @Test
    public void testDeploymentVerificationFailed() {
        String strTmp = System.getProperty("java.io.tmpdir");
        File location = new File(strTmp, "junit.war");
        List<Sniffer> sniffers = Collections.singletonList(new SingleTriggeredSniffer());
        Set<Specification> specifications = Collections.singleton(Specification.SERVLET);
        ArchiveDeployment deployment = new ArchiveDeployment(location.getAbsolutePath(), "customName", specifications, sniffers, "/junit", new HashMap<>());
        Assertions.assertThat(deployment.getDeploymentPhase().isVerified()).isFalse();
        Assertions.assertThat(deployment.getDeploymentPhase().isPrepared()).isFalse();
        Assertions.assertThat(deployment.getDeploymentPhase().isDeployed()).isFalse();

        deployment.setDeploymentLocation(null);
        Assertions.assertThat(deployment.getDeploymentPhase().isVerified()).isFalse();
        Assertions.assertThat(deployment.getDeploymentPhase().isPrepared()).isFalse();
        Assertions.assertThat(deployment.getDeploymentPhase().isDeployed()).isFalse();
        Assertions.assertThat(deployment.hasDeploymentFailed()).isFalse();
    }

    @Test
    public void testDeploymentPrepared() {
        String strTmp = System.getProperty("java.io.tmpdir");
        File archive = new File(strTmp, "junit.war");
        ArchiveDeployment deployment = new ArchiveDeployment(archive);
        Assertions.assertThat(deployment.getDeploymentName()).isEqualTo("junit");
        Assertions.assertThat(deployment.getDeploymentPhase().isVerified()).isTrue();
        Assertions.assertThat(deployment.getDeploymentPhase().isPrepared()).isFalse();
        Assertions.assertThat(deployment.getDeploymentPhase().isDeployed()).isFalse();

        deployment.setArchiveContent(new ArchiveContent.ArchiveContentBuilder().withClassesFiles(new ArrayList<>()).build());
        deployment.setClassLoader(new WebAppClassLoader(archive, deployment.getArchiveContent().getLibraryFiles(), this.getClass().getClassLoader()));
        deployment.setSpecifications(Collections.singleton(Specification.HTML));
        deployment.setSniffers(Collections.singletonList(new SingleTriggeredSniffer()));
        deployment.setDeploymentModule(new Module1());

        Assertions.assertThat(deployment.getDeploymentPhase().isVerified()).isTrue();
        Assertions.assertThat(deployment.getDeploymentPhase().isPrepared()).isTrue();
        Assertions.assertThat(deployment.getDeploymentPhase().isDeployed()).isFalse();
        Assertions.assertThat(deployment.hasDeploymentFailed()).isFalse();
    }

    @Test
    public void testDeploymentLocationAndPrepared() {
        String strTmp = System.getProperty("java.io.tmpdir");
        File location = new File(strTmp, "junit.war");
        List<Sniffer> sniffers = Collections.singletonList(new SingleTriggeredSniffer());
        Set<Specification> specifications = Collections.singleton(Specification.SERVLET);
        ArchiveDeployment deployment = new ArchiveDeployment(location.getAbsolutePath(), "customName", specifications, sniffers, "/junit", new HashMap<>());
        Assertions.assertThat(deployment.getDeploymentPhase().isPrepared()).isFalse();
        Assertions.assertThat(deployment.getDeploymentPhase().isVerified()).isFalse();
        Assertions.assertThat(deployment.getDeploymentPhase().isDeployed()).isFalse();

        // Verification
        deployment.setDeploymentLocation(location);

        deployment.setArchiveContent(new ArchiveContent.ArchiveContentBuilder().withClassesFiles(new ArrayList<>()).build());
        deployment.setClassLoader(new WebAppClassLoader(location, deployment.getArchiveContent().getLibraryFiles(), this.getClass().getClassLoader()));
        deployment.setDeploymentModule(new Module1());

        Assertions.assertThat(deployment.getDeploymentPhase().isVerified()).isTrue();
        Assertions.assertThat(deployment.getDeploymentPhase().isPrepared()).isTrue();
        Assertions.assertThat(deployment.getDeploymentPhase().isDeployed()).isFalse();
        Assertions.assertThat(deployment.hasDeploymentFailed()).isFalse();
    }

    @Test
    public void testSetContextRoot() {
        String strTmp = System.getProperty("java.io.tmpdir");
        File archive = new File(strTmp, "junit.war");
        ArchiveDeployment deployment = new ArchiveDeployment(archive);
        deployment.setContextRoot("/root");
        Assertions.assertThat(deployment.getContextRoot()).isEqualTo("/root");
    }

    @Test
    public void testSetContextRootCleanup() {
        String strTmp = System.getProperty("java.io.tmpdir");
        File archive = new File(strTmp, "junit.war");
        ArchiveDeployment deployment = new ArchiveDeployment(archive);
        deployment.setContextRoot("  root   ");
        Assertions.assertThat(deployment.getContextRoot()).isEqualTo("/root");
    }

    @Test
    public void testSetContextRootNoEndSlash() {
        String strTmp = System.getProperty("java.io.tmpdir");
        File archive = new File(strTmp, "junit.war");
        ArchiveDeployment deployment = new ArchiveDeployment(archive);
        deployment.setContextRoot("root/");
        Assertions.assertThat(deployment.getContextRoot()).isEqualTo("/root");
    }

    @Test
    public void testEquals() {
        String strTmp = System.getProperty("java.io.tmpdir");
        File archive1 = new File(strTmp, "junit.war");
        ArchiveDeployment deployment1 = new ArchiveDeployment(archive1);
        deployment1.setContextRoot("root/");

        File archive2 = new File(strTmp, "junit2.war");
        ArchiveDeployment deployment2 = new ArchiveDeployment(archive2);
        deployment2.setContextRoot("root/");

        Assertions.assertThat(deployment1).isEqualTo(deployment2);

    }

    @Test
    public void testDeploymentFailed() {
        String strTmp = System.getProperty("java.io.tmpdir");
        File archive = new File(strTmp, "junit.war");
        ArchiveDeployment deployment = new ArchiveDeployment(archive);
        deployment.setDeploymentException(new RuntimeException());

        Assertions.assertThat(deployment.hasDeploymentFailed()).isTrue();
    }
}