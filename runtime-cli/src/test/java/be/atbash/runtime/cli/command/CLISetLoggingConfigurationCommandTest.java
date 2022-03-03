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
package be.atbash.runtime.cli.command;

import be.atbash.runtime.cli.command.exception.DomainConnectException;
import be.atbash.runtime.common.command.data.CommandResponse;
import be.atbash.runtime.core.data.exception.message.ExceptionMessageUtil;
import be.atbash.runtime.core.data.parameter.BasicRemoteCLIParameters;
import be.atbash.runtime.logging.testing.LoggingEvent;
import be.atbash.runtime.logging.testing.TestLogMessages;
import be.atbash.util.TestReflectionUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.HttpRequest;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = {8888})
class CLISetLoggingConfigurationCommandTest {

    private MockServerClient client;

    @BeforeEach
    public void beforeEachLifecyleMethod(MockServerClient client) {
        this.client = client;
        ExceptionMessageUtil.addModule("runtime-cli");
    }

    @AfterEach
    private void teardown() {
        TestLogMessages.reset();
        client.reset();
    }

    @Test
    void call() throws Exception {
        TestLogMessages.init();

        CLISetLoggingConfigurationCommand command = new CLISetLoggingConfigurationCommand();
        Integer result = command.call();

        Assertions.assertThat(result).isEqualTo(-1);
        List<LoggingEvent> loggingEvents = TestLogMessages.getLoggingEvents();
        Assertions.assertThat(loggingEvents).hasSize(1);
        Assertions.assertThat(loggingEvents.get(0).getLevel()).isEqualTo(Level.WARNING);
        Assertions.assertThat(loggingEvents.get(0).getMessage()).startsWith("RCLI-011: ");

    }

    @Test
    void call_withKeyValue() throws Exception {
        TestLogMessages.init(true);

        CLISetLoggingConfigurationCommand command = new CLISetLoggingConfigurationCommand();
        TestReflectionUtils.setFieldValue(command, "options", new String[]{"key=value}"});
        TestReflectionUtils.resetOf(command, "propertiesFile");

        BasicRemoteCLIParameters remoteParameters = new BasicRemoteCLIParameters();
        remoteParameters.setPort(8888);
        remoteParameters.setHost("localhost");
        TestReflectionUtils.setFieldValue(command, "basicRemoteCLIParameters", remoteParameters);

        CommandResponse commandResponse = new CommandResponse();
        commandResponse.setSuccess(true);
        commandResponse.getData().put("result", "All Good");

        ObjectMapper mapper = new ObjectMapper();

        HttpRequest httpRequest = request()
                .withMethod("POST")
                .withPath("/domain/set-logging-configuration");

        client.when(httpRequest)
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(mapper.writeValueAsString(commandResponse).getBytes())

                );


        Integer result = command.call();

        Assertions.assertThat(result).isEqualTo(0);
        List<LoggingEvent> loggingEvents = TestLogMessages.getLoggingEvents();
        Assertions.assertThat(loggingEvents).hasSize(2);
        Assertions.assertThat(loggingEvents.get(0).getMessage()).isEqualTo("result: All Good");
        Assertions.assertThat(loggingEvents.get(1).getMessage()).isEqualTo("Command execution successful");


        HttpRequest[] requests = client.retrieveRecordedRequests(httpRequest);
        Assertions.assertThat(requests).hasSize(1);
        Assertions.assertThat(requests[0].getBodyAsJsonOrXmlString()).isEqualTo("=key%3Dvalue%7D");
    }

    @Test
    void call_serverReportFailure() throws Exception {
        TestLogMessages.init(true);

        CLISetLoggingConfigurationCommand command = new CLISetLoggingConfigurationCommand();
        TestReflectionUtils.setFieldValue(command, "options", new String[]{"key=value}"});
        TestReflectionUtils.resetOf(command, "propertiesFile");

        BasicRemoteCLIParameters remoteParameters = new BasicRemoteCLIParameters();
        remoteParameters.setPort(8888);
        remoteParameters.setHost("localhost");
        TestReflectionUtils.setFieldValue(command, "basicRemoteCLIParameters", remoteParameters);

        HttpRequest httpRequest = request()
                .withMethod("POST")
                .withPath("/domain/set-logging-configuration");

        client.when(httpRequest)
                .respond(
                        response()
                                .withStatusCode(404)
                                .withBody("Unknown endpoint")

                );

        Assertions.assertThatThrownBy(command::call).isInstanceOf(DomainConnectException.class)
                .hasMessage("RC-010: Unable to contact Runtime domain endpoint");


        List<LoggingEvent> loggingEvents = TestLogMessages.getLoggingEvents();
        Assertions.assertThat(loggingEvents).hasSize(1);
        Assertions.assertThat(loggingEvents.get(0).getMessage()).isEqualTo("RC-211: Calling Runtime domain endpoint resulted in status 404 (message 'Unknown endpoint')");

        HttpRequest[] requests = client.retrieveRecordedRequests(httpRequest);
        Assertions.assertThat(requests).hasSize(1);
        Assertions.assertThat(requests[0].getBodyAsJsonOrXmlString()).isEqualTo("=key%3Dvalue%7D");
    }

    @Test
    void call_serverReportWrongOptionsUsed() throws Exception {
        TestLogMessages.init(true);

        CLISetLoggingConfigurationCommand command = new CLISetLoggingConfigurationCommand();
        TestReflectionUtils.setFieldValue(command, "options", new String[]{"key}"});
        TestReflectionUtils.resetOf(command, "propertiesFile");

        BasicRemoteCLIParameters remoteParameters = new BasicRemoteCLIParameters();
        remoteParameters.setPort(8888);
        remoteParameters.setHost("localhost");
        TestReflectionUtils.setFieldValue(command, "basicRemoteCLIParameters", remoteParameters);

        HttpRequest httpRequest = request()
                .withMethod("POST")
                .withPath("/domain/set-logging-configuration");

        CommandResponse commandResponse = new CommandResponse();
        commandResponse.setSuccess(false);
        commandResponse.setErrorMessage("CONFIG-101: Option must be 2 parts separated by =, received 'key'");

        ObjectMapper mapper = new ObjectMapper();

        client.when(httpRequest)
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(mapper.writeValueAsString(commandResponse).getBytes())

                );

        Integer result = command.call();
        Assertions.assertThat(result).isEqualTo(0);

        List<LoggingEvent> loggingEvents = TestLogMessages.getLoggingEvents();
        Assertions.assertThat(loggingEvents).hasSize(2);
        Assertions.assertThat(loggingEvents.get(0).getMessage()).isEqualTo("Command execution failed with the following message");
        Assertions.assertThat(loggingEvents.get(1).getMessage()).isEqualTo("CONFIG-101: Option must be 2 parts separated by =, received 'key'");

        HttpRequest[] requests = client.retrieveRecordedRequests(httpRequest);
        Assertions.assertThat(requests).hasSize(1);
        Assertions.assertThat(requests[0].getBodyAsJsonOrXmlString()).isEqualTo("=key%7D");
    }

    @Test
    void call_withFile() throws Exception {
        TestLogMessages.init(true);

        CLISetLoggingConfigurationCommand command = new CLISetLoggingConfigurationCommand();

        File file = new File("./target/test-classes/logging.properties");

        // Test to make sure the file exists.
        Assertions.assertThat(file).as("Check failed for dependent file").exists();
        TestReflectionUtils.setFieldValue(command, "propertiesFile", file);

        BasicRemoteCLIParameters remoteParameters = new BasicRemoteCLIParameters();
        remoteParameters.setPort(8888);
        remoteParameters.setHost("localhost");
        TestReflectionUtils.setFieldValue(command, "basicRemoteCLIParameters", remoteParameters);

        CommandResponse commandResponse = new CommandResponse();
        commandResponse.setSuccess(true);
        commandResponse.getData().put("result", "All Good");

        ObjectMapper mapper = new ObjectMapper();

        HttpRequest httpRequest = request()
                .withMethod("POST")
                .withPath("/domain/set-logging-configuration");

        client.when(httpRequest)
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(mapper.writeValueAsString(commandResponse).getBytes())

                );


        Integer result = command.call();

        Assertions.assertThat(result).isEqualTo(0);
        List<LoggingEvent> loggingEvents = TestLogMessages.getLoggingEvents();
        Assertions.assertThat(loggingEvents).hasSize(2);
        Assertions.assertThat(loggingEvents.get(0).getMessage()).isEqualTo("result: All Good");
        Assertions.assertThat(loggingEvents.get(1).getMessage()).isEqualTo("Command execution successful");


        HttpRequest[] requests = client.retrieveRecordedRequests(httpRequest);
        Assertions.assertThat(requests).hasSize(1);
        String bodyAsJsonOrXmlString = requests[0].getBodyAsJsonOrXmlString();
        Assertions.assertThat(bodyAsJsonOrXmlString).startsWith("--AtbashRuntimeUploadBoundary");
        Assertions.assertThat(bodyAsJsonOrXmlString).contains("key=value In File");
    }

    @Test
    void call_withNonExistingFile() throws Exception {
        TestLogMessages.init(true);

        CLISetLoggingConfigurationCommand command = new CLISetLoggingConfigurationCommand();

        File file = new File("./target/test-classes/someRandomFile");

        // Test to make sure the file exists.
        Assertions.assertThat(file).as("Check failed for dependent file").doesNotExist();

        TestReflectionUtils.setFieldValue(command, "propertiesFile", file);

        // BasicRemoteCLIParameters not needed as we never get to the point where we call the Domain endpoint.


        Integer result = command.call();

        Assertions.assertThat(result).isEqualTo(0);
        List<LoggingEvent> loggingEvents = TestLogMessages.getLoggingEvents();
        Assertions.assertThat(loggingEvents).hasSize(1);
        Assertions.assertThat(loggingEvents.get(0).getLevel()).isEqualTo(Level.WARNING);
        Assertions.assertThat(loggingEvents.get(0).getMessage()).isEqualTo("DEPLOY-105");


    }

}