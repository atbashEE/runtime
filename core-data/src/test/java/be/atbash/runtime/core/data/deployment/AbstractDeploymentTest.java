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

import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.logging.mapping.BundleMapping;
import be.atbash.runtime.logging.testing.TestLogMessages;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractDeploymentTest {

    @Mock
    private Module moduleMock;

    @AfterEach
    public void teardown() {
        TestLogMessages.reset();
    }

    @BeforeAll
    public static void setup() {
        // So that logging messages can be looked up
        BundleMapping.getInstance().addMapping(TestDeployment.class.getName(), AbstractDeployment.class.getName());
    }
    @Test
    void init() {
        TestDeployment deployment = new TestDeployment("name", "/root", new HashMap<>());
        Assertions.assertThat(deployment.getDeploymentPhase()).isEqualTo(DeploymentPhase.NOT_STARTED);
    }

    @ParameterizedTest
    @MethodSource("setApplicationReady_TestData")
    void setApplicationReady(DeploymentPhase phase, boolean deploymentInitiated, boolean expected) {
        TestLogMessages.init();
        TestDeployment deployment = new TestDeployment("name", "/root", new HashMap<>());
        deployment.forceDeploymentPhase(phase);
        if (deploymentInitiated) {
            deployment.setDeployInitiated();
        }
        deployment.setApplicationReady();
        if (expected) {
            Assertions.assertThat(deployment.getDeploymentPhase()).isEqualTo(DeploymentPhase.READY);
            Assertions.assertThat(TestLogMessages.getLoggingEvents()).isEmpty();
        } else {
            Assertions.assertThat(deployment.getDeploymentPhase()).isEqualTo(phase);
            Assertions.assertThat(TestLogMessages.getLoggingEvents()).hasSize(1);
            Assertions.assertThat(TestLogMessages.getLoggingEvents().get(0).getLevel()).isEqualTo(Level.SEVERE);
        }
    }

    @ParameterizedTest
    @MethodSource("setContextRoot_TestData")
    void setContextRoot(DeploymentPhase phase, boolean deploymentInitiated, boolean expected) {
        TestLogMessages.init();
        TestDeployment deployment = new TestDeployment("name", "/root", new HashMap<>());
        deployment.forceDeploymentPhase(phase);
        if (deploymentInitiated) {
            deployment.setDeployInitiated();
        }
        deployment.setContextRoot("/new");
        if (expected) {
            Assertions.assertThat(deployment.getContextRoot()).isEqualTo("/new");
            Assertions.assertThat(TestLogMessages.getLoggingEvents()).isEmpty();
        } else {
            Assertions.assertThat(deployment.getContextRoot()).isEqualTo("/root");
            Assertions.assertThat(TestLogMessages.getLoggingEvents()).hasSize(1);
            Assertions.assertThat(TestLogMessages.getLoggingEvents().get(0).getLevel()).isEqualTo(Level.SEVERE);
        }
    }

    @Test
    void setDeploymentModule() {
        TestDeployment deployment = new TestDeployment("name", "/root", new HashMap<>());
        deployment.setDeploymentModule(moduleMock);
        Assertions.assertThat(deployment.getDeploymentPhase()).isEqualTo(DeploymentPhase.NOT_STARTED);
    }

    @ParameterizedTest
    @MethodSource("setDeployed_TestData")
    void setDeployed(DeploymentPhase phase, boolean expected) {
        TestDeployment deployment = new TestDeployment("name", "/root", new HashMap<>());
        deployment.forceDeploymentPhase(phase);
        deployment.setDeployed();

        if (expected) {
            Assertions.assertThat(deployment.getDeploymentPhase()).isEqualTo(DeploymentPhase.DEPLOYED);
        } else {
            Assertions.assertThat(deployment.getDeploymentPhase()).isEqualTo(phase);
        }
    }

    @Test
    void setDeploymentException() {
        TestDeployment deployment = new TestDeployment("name", "/root", new HashMap<>());

        Assertions.assertThat(deployment.hasDeploymentFailed()).isFalse();
        deployment.setDeploymentException(new Exception());

        Assertions.assertThat(deployment.getDeploymentPhase()).isEqualTo(DeploymentPhase.FAILED);
        Assertions.assertThat(deployment.getDeploymentException()).isNotNull();
        Assertions.assertThat(deployment.hasDeploymentFailed()).isTrue();
    }

    static Stream<Arguments> setApplicationReady_TestData() {
        return Stream.of(
                Arguments.of(DeploymentPhase.NOT_STARTED, false, false),
                Arguments.of(DeploymentPhase.NOT_STARTED, true, false),

                Arguments.of(DeploymentPhase.VERIFIED, false, false),
                Arguments.of(DeploymentPhase.VERIFIED, true, false),

                Arguments.of(DeploymentPhase.PREPARED, false, false),
                Arguments.of(DeploymentPhase.PREPARED, true, true),

                Arguments.of(DeploymentPhase.DEPLOYED, false, true),
                Arguments.of(DeploymentPhase.DEPLOYED, true, true),

                Arguments.of(DeploymentPhase.FAILED, false, false),
                Arguments.of(DeploymentPhase.FAILED, true, false)

        );
    }

    static Stream<Arguments> setContextRoot_TestData() {
        return Stream.of(
                Arguments.of(DeploymentPhase.NOT_STARTED, false, true),
                Arguments.of(DeploymentPhase.NOT_STARTED, true, true),

                Arguments.of(DeploymentPhase.VERIFIED, false, true),
                Arguments.of(DeploymentPhase.VERIFIED, true, true),

                Arguments.of(DeploymentPhase.PREPARED, false, true),
                Arguments.of(DeploymentPhase.PREPARED, true, false),

                Arguments.of(DeploymentPhase.DEPLOYED, false, false),
                Arguments.of(DeploymentPhase.DEPLOYED, true, false),

                Arguments.of(DeploymentPhase.FAILED, false, false),
                Arguments.of(DeploymentPhase.FAILED, true, false)

        );
    }

    static Stream<Arguments> setDeployed_TestData() {
        return Stream.of(
                Arguments.of(DeploymentPhase.NOT_STARTED, true),
                Arguments.of(DeploymentPhase.VERIFIED, true),
                Arguments.of(DeploymentPhase.PREPARED, true),
                Arguments.of(DeploymentPhase.DEPLOYED, true),
                Arguments.of(DeploymentPhase.READY, false),
                Arguments.of(DeploymentPhase.FAILED, false)
        );
    }

    static class TestDeployment extends AbstractDeployment {

        public TestDeployment(String deploymentName, String contextRoot, Map<String, String> deploymentData) {
            super(deploymentName, contextRoot, deploymentData);
        }

        void forceDeploymentPhase(DeploymentPhase deploymentPhase) {
            this.deploymentPhase = deploymentPhase;
        }
    }
}