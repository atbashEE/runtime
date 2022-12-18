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
package be.atbash.runtime.jetty;

import be.atbash.runtime.core.data.RunData;
import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.assertj.core.api.Assertions;
import org.eclipse.jetty.server.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

@ExtendWith(MockitoExtension.class)
class HealthHandlerTest {

    @Mock
    private Request baseRequestMock;

    @Mock
    private HttpServletRequest requestMock;

    @Mock
    private HttpServletResponse responseMock;

    @Mock
    private PrintWriter writerMock;

    @Captor
    private ArgumentCaptor<String> contentCaptor;

    @Test
    void handle() throws ServletException, IOException {
        Mockito.when(responseMock.getWriter()).thenReturn(writerMock);
        RunData runData = new RunData();

        // Fake a successful deployment
        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .build();

        ArchiveDeployment deployment = new ArchiveDeployment(new File("./applications/test.war"));
        deployment.setDeployed();

        runData.deployed(deployment);
        deployment.setApplicationReady();

        HealthHandler handler = new HealthHandler(runData);
        handler.handle("/health", baseRequestMock, requestMock, responseMock);

        Mockito.verify(writerMock).println(contentCaptor.capture());
        Assertions.assertThat(contentCaptor.getValue()).isEqualToIgnoringWhitespace("{\n" +
                "\"status\":\"UP\",\n" +
                "\"checks\":[\n" +
                "{\n" +
                "\"name\":\"applications\",\n" +
                "\"status\":\"UP\",\n" +
                "\"data\":[\n" +
                "\"test\"\n" +
                "]\n" +
                "}\n" +
                "]\n" +
                "}");
    }

    @Test
    void handleFailed() throws ServletException, IOException {
        Mockito.when(responseMock.getWriter()).thenReturn(writerMock);
        RunData runData = new RunData();

        // Fake a successful deployment
        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .build();

        ArchiveDeployment deployment = new ArchiveDeployment(new File("./applications/test.war"));
        deployment.setDeploymentException(new RuntimeException());

        runData.failedDeployment(deployment);

        HealthHandler handler = new HealthHandler(runData);
        handler.handle("/health", baseRequestMock, requestMock, responseMock);

        Mockito.verify(writerMock).println(contentCaptor.capture());
        Assertions.assertThat(contentCaptor.getValue()).isEqualToIgnoringWhitespace("{\n" +
                "\"status\": \"DOWN\",\n" +
                "\"checks\": []\n" +
                "}");
    }

    @Test
    void handle_noapps() throws ServletException, IOException {
        Mockito.when(responseMock.getWriter()).thenReturn(writerMock);
        RunData runData = new RunData();

        HealthHandler handler = new HealthHandler(runData);
        handler.handle("/health", baseRequestMock, requestMock, responseMock);

        Mockito.verify(writerMock).println(contentCaptor.capture());
        Assertions.assertThat(contentCaptor.getValue()).isEqualToIgnoringWhitespace("{\n" +
                "\"status\": \"DOWN\",\n" +
                "\"checks\": []\n" +
                "}");
    }

    @Test
    void handle_noApps_domain() throws ServletException, IOException {
        Mockito.when(responseMock.getWriter()).thenReturn(writerMock);
        RunData runData = new RunData();
        runData.setDomainMode();

        HealthHandler handler = new HealthHandler(runData);
        handler.handle("/health", baseRequestMock, requestMock, responseMock);

        Mockito.verify(writerMock).println(contentCaptor.capture());
        Assertions.assertThat(contentCaptor.getValue()).isEqualToIgnoringWhitespace("{\n" +
                "\"status\":\"UP\",\n" +
                "\"checks\":[\n" +
                "{\n" +
                "\"name\":\"applications\",\n" +
                "\"status\":\"UP\",\n" +
                "\"data\":[\n" +
                "\n" +
                "]\n" +
                "}\n" +
                "]\n" +
                "}");
    }
}