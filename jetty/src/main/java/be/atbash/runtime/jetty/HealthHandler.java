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
package be.atbash.runtime.jetty;

import be.atbash.runtime.core.data.RunData;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.IOException;
import java.util.stream.Collectors;

public class HealthHandler extends AbstractHandler {

    private final RunData runData;

    public HealthHandler(RunData runData) {

        this.runData = runData;
    }

    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!target.startsWith("/health")) {
            return;
        }
        baseRequest.setHandled(true);
        response.setContentType("text/html;charset=utf-8");

        boolean activeApplications = hasActiveApplications();

        if (!runData.isDomainMode() && !activeApplications) {
            downWithNoApplications(response);
        } else {
            withApplications(response, runData);
        }
    }

    private boolean hasActiveApplications() {
        return runData.getDeployments().stream()
                .anyMatch(ad -> !ad.hasDeploymentFailed());
    }

    private void withApplications(HttpServletResponse response, RunData runData) throws IOException {
        String readyNames = runData.getDeployments()
                .stream()
                .filter(ad ->  ad.getDeploymentPhase().isReady())
                .map(ad -> "\"" + ad.getDeploymentName() + "\"")
                .collect(Collectors.joining(","));
        String notReadyNames = runData.getDeployments()
                .stream()
                .filter(ad -> !ad.getDeploymentPhase().isReady())
                .map(ad -> "\"" + ad.getDeploymentName() + "\"")
                .collect(Collectors.joining(","));
        String overallStatus;
        if (notReadyNames.isEmpty()) {
            overallStatus = "UP";
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            overallStatus = "DOWN";
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }

        String responseBody = "{\n" +
                "   \"status\":\"" + overallStatus + "\",\n" +
                "   \"checks\":[\n";
        if (!readyNames.isEmpty()) {
            responseBody = responseBody +
                    "      {\n" +
                    "         \"name\":\"applications\",\n" +
                    "         \"status\":\"UP\",\n" +
                    "         \"data\":[\n" +
                    "            " + readyNames + "\n" +
                    "         ]\n" +
                    "      }\n";
        }
        if (!notReadyNames.isEmpty()) {
            responseBody = responseBody +
                    "      {\n" +
                    "         \"name\":\"applications\",\n" +
                    "         \"status\":\"DOWN\",\n" +
                    "         \"data\":[\n" +
                    "            " + notReadyNames + "\n" +
                    "         ]\n" +
                    "      }\n";
        }
        if (runData.getDeployments().isEmpty()) {
            responseBody = responseBody +
                    "    {\n" +
                    "      \"name\":\"applications\",\n" +
                    "      \"status\":\"UP\",\n" +
                    "      \"data\":[\n" +
                    "     ]\n" +
                    "    }\n";

        }
        responseBody = responseBody +
                "     ]\n" +
                "  }\n";
        response.getWriter().println(responseBody);
    }

    private void downWithNoApplications(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.getWriter().println("{\n" +
                "  \"status\": \"DOWN\",\n" +
                "  \"checks\": []\n" +
                "}");
    }
}
