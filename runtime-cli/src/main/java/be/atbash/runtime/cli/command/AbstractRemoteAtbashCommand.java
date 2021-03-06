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
import be.atbash.runtime.cli.command.util.MultipartBodyPublisher;
import be.atbash.runtime.common.command.AbstractAtbashCommand;
import be.atbash.runtime.common.command.data.CommandResponse;
import be.atbash.runtime.core.data.exception.UnexpectedException;
import be.atbash.runtime.core.data.parameter.BasicRemoteCLIParameters;
import be.atbash.runtime.core.data.parameter.RemoteCLIOutputFormat;
import be.atbash.runtime.core.data.util.ArchiveDeploymentUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import static be.atbash.runtime.common.command.RuntimeCommonConstant.CLASS_INFO_MARKER;

/**
 * Abstract class for all Remote CLI commands as it contains the code to call the Runtime endpoint.
 */
public abstract class AbstractRemoteAtbashCommand extends AbstractAtbashCommand {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @CommandLine.Mixin
    protected BasicRemoteCLIParameters basicRemoteCLIParameters;

    void callRemoteCLI(String method, String command, BasicRemoteCLIParameters remoteCLIParameters, Map<String, String> options) {

        HttpClient client = HttpClient.newHttpClient();

        try {
            HttpRequest request;
            URI uri;
            if ("POST".equals(method)) {
                uri = assembleURI(command, remoteCLIParameters);

                String body = ParameterStringBuilder.getParamsString(options);

                request = HttpRequest.newBuilder()
                        .uri(uri)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
            } else {
                uri = assembleURI(command, remoteCLIParameters, options);

                request = HttpRequest.newBuilder()
                        .uri(uri)
                        .GET()
                        .build();
            }

            HttpResponse<String> response;
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (ConnectException e) {

                LOGGER.error("RC-210: Unable to contact Runtime domain endpoint.");
                throw new DomainConnectException(e);
            }

            String data = response.body();
            int statusCode = response.statusCode();
            if (statusCode != 200) {
                LOGGER.error("RC-211: Calling Runtime domain endpoint resulted in status {} (message '{}')", statusCode, data);
                throw new DomainConnectException(null);
            }
            writeCommandResult(remoteCLIParameters, data);

        } catch (InterruptedException e) {
            // re-interrupt so that we have a proper cleanup
            Thread.currentThread().interrupt();
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        } catch (IOException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }
    }

    private void writeCommandResult(BasicRemoteCLIParameters remoteCLIParameters, String data) throws JsonProcessingException {
        if (remoteCLIParameters.getFormat() == RemoteCLIOutputFormat.JSON) {
            LOGGER.info(data);
        } else {
            ObjectMapper mapper = new ObjectMapper();
            CommandResponse commandResponse = mapper.readValue(data, CommandResponse.class);
            if (commandResponse.isSuccess()) {
                writeCommandOutput(commandResponse);
                LOGGER.info("Command execution successful");
            } else {
                writeErrorMessage(commandResponse);
            }
        }

    }

    private void writeErrorMessage(CommandResponse commandResponse) {

        LOGGER.error("Command execution failed with the following message");
        LOGGER.error(commandResponse.getErrorMessage());
    }

    private void writeCommandOutput(CommandResponse commandResponse) {
        if (commandResponse.getData().containsKey(CLASS_INFO_MARKER)) {
            writeOutputCommandWithComplexData(commandResponse);
        } else {
            for (Map.Entry<String, String> entry : commandResponse.getData().entrySet()) {

                LOGGER.info(entry.getKey() + ": " + entry.getValue());
            }
        }
    }

    private void writeOutputCommandWithComplexData(CommandResponse commandResponse) {
        try {
            Class<?> infoClass = Class.forName(commandResponse.getData().get(CLASS_INFO_MARKER));
            ObjectMapper mapper = new ObjectMapper();
            for (Map.Entry<String, String> entry : commandResponse.getData().entrySet()) {
                if (entry.getKey().equals(CLASS_INFO_MARKER)) {
                    continue;
                }
                Object value = mapper.readValue(entry.getValue(), infoClass);

                LOGGER.info(entry.getKey() + ": " + value.toString());
            }
        } catch (ClassNotFoundException | JsonProcessingException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }
    }

    private URI assembleURI(String command, BasicRemoteCLIParameters remoteCLIParameters) throws MalformedURLException {
        String result = determineBaseURLForCommand(command, remoteCLIParameters);
        return URI.create(result);
    }

    private URI assembleURI(String command, BasicRemoteCLIParameters remoteCLIParameters, Map<String, String> options) throws MalformedURLException, UnsupportedEncodingException {
        String result = determineBaseURLForCommand(command, remoteCLIParameters) + "?" + ParameterStringBuilder.getParamsString(options);
        return URI.create(result);
    }

    private String determineBaseURLForCommand(String command, BasicRemoteCLIParameters remoteCLIParameters) {
        return "http://" + remoteCLIParameters.getHost() +
                ":" +
                remoteCLIParameters.getPort() +
                "/domain/" +  // FIXME domain must be configurable.
                command;
    }

    void callRemoteCLI(String command, BasicRemoteCLIParameters remoteCLIParameters, Map<String, String> options, boolean isWar, File[] archives) {

        if (archives == null) {
            LOGGER.warn("RC-108: The command is missing one or more file arguments.");
            return;
        }
        for (File file : archives) {
            if (!ArchiveDeploymentUtil.testOnArchive(file, isWar)) {
                return;
            }
        }
        try {
            HttpClient client = HttpClient.newHttpClient();

            URI uri = assembleURI(command, remoteCLIParameters, options);

            MultipartBodyPublisher publisher = new MultipartBodyPublisher();

            Arrays.stream(archives).forEach(
                    archive -> {
                        String name = archive.getName();
                        publisher.addPart(name, archive.toPath());
                    }
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Content-Type", "multipart/form-data; boundary=" + MultipartBodyPublisher.BOUNDARY)
                    .POST(publisher.build())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String data = response.body();

            int statusCode = response.statusCode();
            if (statusCode != 200) {
                LOGGER.error("CLI-211: Calling Runtime domain endpoint resulted in status {} (message '{}')", statusCode, data);
                throw new DomainConnectException(null);
            }
            writeCommandResult(remoteCLIParameters, data);

        } catch (IOException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        } catch (InterruptedException e) {
            // re-interrupt so that we have a proper cleanup
            Thread.currentThread().interrupt();
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }
    }

    private static class ParameterStringBuilder {
        public static String getParamsString(Map<String, String> params)
                throws UnsupportedEncodingException {
            StringBuilder result = new StringBuilder();

            for (Map.Entry<String, String> entry : params.entrySet()) {
                result.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                result.append("&");
            }

            String resultString = result.toString();
            return resultString.length() > 0
                    ? resultString.substring(0, resultString.length() - 1)
                    : resultString;
        }
    }
}
