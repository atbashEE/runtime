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
package be.atbash.runtime.remotecli;

import be.atbash.runtime.common.command.data.CommandResponse;
import be.atbash.runtime.remotecli.command.*;
import be.atbash.runtime.remotecli.exception.IncorrectContentTypeWithCommandException;
import be.atbash.runtime.remotecli.exception.UnknownCommandException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DomainHandler extends AbstractHandler {

    public static final String DOMAIN_URL = "/domain/";  // FIXME Configurable?
    private static final MultipartConfigElement MULTI_PART_CONFIG = new MultipartConfigElement("temp/upload");

    private final Map<String, ServerRemoteCommand> commands;

    public DomainHandler() {
        commands = new HashMap<>();
        commands.put("status", new StatusRemoteCommand());
        commands.put("deploy", new DeployRemoteCommand());
        commands.put("list-applications", new ListApplicationsRemoteCommand());
        commands.put("undeploy", new UndeployRemoteCommand());
    }

    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!target.startsWith(DOMAIN_URL)) {
            return;
        }
        baseRequest.setHandled(true);
        response.setContentType("application/json");

        String command = determineCommand(request.getRequestURI());
        ServerRemoteCommand remoteCommand;
        if (commands.containsKey(command)) {
            remoteCommand = commands.get(command);
        } else {
            throw new UnknownCommandException(command);
        }

        Map<String, String> options;

        if ("GET".equals(request.getMethod())) {
            options = retrieveOptionsFromURL(request);
        } else {
            String contentType = request.getContentType();

            if (contentType != null && contentType.startsWith("multipart/")) {
                if (!(remoteCommand instanceof HandleFileUpload)) {
                    throw new IncorrectContentTypeWithCommandException(command);
                }

                options = retrieveOptionsFromURL(request);
                request.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, MULTI_PART_CONFIG);
                for (Part part : request.getParts()) {
                    ((HandleFileUpload) remoteCommand).uploadedFile(part.getName(), part.getInputStream());
                }
            } else {
                String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
                options = new HashMap<>();

                Pattern.compile("&")
                        .splitAsStream(body)
                        .map(s -> Arrays.copyOf(s.split("=", 2), 2))
                        .map(o -> Map.entry(decode(o[0]), decode(o[1])))
                        .forEach(e -> options.put(e.getKey(), e.getValue()));
            }
        }
        CommandResponse result = remoteCommand.handleCommand(options);

        ObjectMapper mapper = new ObjectMapper();
        response.getOutputStream().println(mapper.writeValueAsString(result));
    }

    private Map<String, String> retrieveOptionsFromURL(HttpServletRequest request) {
        Map<String, String> options = new HashMap<>();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            String key = URLDecoder.decode(entry.getKey(), StandardCharsets.UTF_8);
            String value = "";
            if (entry.getValue().length > 0) {
                value = URLDecoder.decode(entry.getValue()[0], StandardCharsets.UTF_8);
            }
            options.put(key, value);
        }
        return options;
    }

    private String determineCommand(String requestURI) {
        return requestURI.substring(DOMAIN_URL.length());
    }

    private static String decode(final String encoded) {
        return Optional.ofNullable(encoded)
                .map(e -> URLDecoder.decode(e, StandardCharsets.UTF_8))
                .orElse(null);
    }
}
