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
package be.atbash.runtime.common.command;

import be.atbash.runtime.common.command.data.CommandResponse;
import be.atbash.runtime.common.command.exception.DomainConnectException;
import be.atbash.runtime.common.command.util.MultipartBodyPublisher;
import be.atbash.runtime.core.data.parameter.BasicRemoteCLIParameters;
import be.atbash.runtime.core.data.parameter.RemoteCLIOutputFormat;
import be.atbash.runtime.core.exception.UnexpectedException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine;

import java.io.*;
import java.net.*;
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

    @CommandLine.Mixin
    protected BasicRemoteCLIParameters basicRemoteCLIParameters;

    @Override
    public CommandType getCommandType() {
        return CommandType.CLI;
    }

    void callRemoteCLI(String method, String command, BasicRemoteCLIParameters remoteCLIParameters, Map<String, String> options) {

        // FIXME rewrite using the JDK 9 HttpClient.
        URL url = null;
        try {
            if ("POST".equals(method)) {
                url = assembleURL(command, remoteCLIParameters);
            } else {
                url = assembleURL(command, remoteCLIParameters, options);
            }

            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod(method);
            try {
                con.connect();
            } catch (ConnectException e) {
                // FIXME  is System.out the best
                System.out.println("CLI-210: Unable to contact Runtime domain endpoint.");
                throw new DomainConnectException("Unable to contact Runtime domain endpoint.", e);
            }

            if ("POST".equals(method)) {
                con.setDoOutput(true);
                DataOutputStream out = new DataOutputStream(con.getOutputStream());
                out.writeBytes(ParameterStringBuilder.getParamsString(options));
                out.flush();
                out.close();
            }

            int status = con.getResponseCode();
            // FIXME check for status 200

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            con.disconnect();

            String data = content.toString();

            writeCommandResult(remoteCLIParameters, data);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void writeCommandResult(BasicRemoteCLIParameters remoteCLIParameters, String data) throws JsonProcessingException {
        if (remoteCLIParameters.getFormat() == RemoteCLIOutputFormat.JSON) {
            System.out.println(data);  // FIXME  System.out or Logger?
        } else {
            ObjectMapper mapper = new ObjectMapper();
            CommandResponse commandResponse = mapper.readValue(data, CommandResponse.class);
            if (commandResponse.isSuccess()) {
                writeCommandOutput(commandResponse);
            } else {
                writeErrorMessage(commandResponse);
            }
        }

    }

    private void writeErrorMessage(CommandResponse commandResponse) {
        // FIXME System.out of Logger?
        System.out.println("Command execution failed with the following message");
        System.out.println(commandResponse.getErrorMessage());
    }

    private void writeCommandOutput(CommandResponse commandResponse) {
        if (commandResponse.getData().containsKey(CLASS_INFO_MARKER)) {
            writeOutputCommandWithComplexData(commandResponse);
        } else {
            for (Map.Entry<String, String> entry : commandResponse.getData().entrySet()) {
                // FIXME System.out of Logger?
                System.out.println(entry.getKey() + ": " + entry.getValue());
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
                // FIXME System.out of Logger?
                System.out.println(entry.getKey() + ": " + value.toString());
            }
        } catch (ClassNotFoundException | JsonProcessingException e) {
            e.printStackTrace();  // FIXME
        }
    }

    private URL assembleURL(String command, BasicRemoteCLIParameters remoteCLIParameters) throws MalformedURLException {
        String result = determineBaseURLForCommand(command, remoteCLIParameters);
        return new URL(result);
    }

    private URL assembleURL(String command, BasicRemoteCLIParameters remoteCLIParameters, Map<String, String> options) throws MalformedURLException, UnsupportedEncodingException {
        String result = determineBaseURLForCommand(command, remoteCLIParameters) + "?" + ParameterStringBuilder.getParamsString(options);
        return new URL(result);
    }

    private String determineBaseURLForCommand(String command, BasicRemoteCLIParameters remoteCLIParameters) {
        return "http://" + remoteCLIParameters.getHost() +
                ":" +
                remoteCLIParameters.getPort() +
                "/domain/" +  // FIXME domain must be configurable.
                command;
    }

    void callRemoteCLI(String command, BasicRemoteCLIParameters remoteCLIParameters, Map<String, String> options, File[] archives) {

        try {
            HttpClient client = HttpClient.newHttpClient();

            URI uri = assembleURL(command, remoteCLIParameters, options).toURI();

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

            int status = response.statusCode();
            // FIXME check on status

            String data = response.body();
            writeCommandResult(remoteCLIParameters, data);

        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new UnexpectedException(e);
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
