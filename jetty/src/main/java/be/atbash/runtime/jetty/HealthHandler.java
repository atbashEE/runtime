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

    private RunData runData;

    public HealthHandler(RunData runData) {

        this.runData = runData;
    }

    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!target.startsWith("/health")) {
            return;
        }
        baseRequest.setHandled(true);
        response.setContentType("text/html;charset=utf-8");

        int status;
        if (runData.isDomainMode()) {
            status = HttpServletResponse.SC_OK;
        } else {
            status = runData.getDeployments().isEmpty() ? HttpServletResponse.SC_SERVICE_UNAVAILABLE : HttpServletResponse.SC_OK;
        }
        response.setStatus(status);

        if (!runData.isDomainMode() && runData.getDeployments().isEmpty()) {
            downWithNoApplications(response);
        } else {
            upWithApplications(response, runData);
        }
    }

    private void upWithApplications(HttpServletResponse response, RunData runData) throws IOException {
        String names = runData.getDeployments()
                .stream()
                .map(ad -> "\"" + ad.getDeploymentName() + "\"")
                .collect(Collectors.joining(","));
        response.getWriter().println("{\n" +
                "   \"status\":\"UP\",\n" +
                "   \"checks\":[\n" +
                "      {\n" +
                "         \"name\":\"applications\",\n" +
                "         \"status\":\"UP\",\n" +
                "         \"data\":[\n" +
                "            " + names + "\n" +
                "         ]\n" +
                "      }\n" +
                "   ]\n" +
                "}");
    }

    private void downWithNoApplications(HttpServletResponse response) throws IOException {
        response.getWriter().println("{\n" +
                "  \"status\": \"DOWN\",\n" +
                "  \"checks\": []\n" +
                "}");
    }
}
