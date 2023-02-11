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
package be.atbash.runtime.metrics.jetty;

import be.atbash.runtime.metrics.MetricsDataProvider;
import be.atbash.runtime.metrics.MetricsDataProviderConsumer;
import be.atbash.runtime.metrics.collector.Percentiles;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class AtbashMetricsHandler extends AbstractHandler implements MetricsDataProviderConsumer {

    private MetricsDataProvider provider;

    @Override
    public void setProvider(MetricsDataProvider provider) {
        this.provider = provider;
    }

    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!target.startsWith("/metrics/atbash")) {
            return;
        }
        baseRequest.setHandled(true);
        response.setContentType("text/html;charset=utf-8");

        PrintWriter writer = response.getWriter();
        writer.println("<h2>Atbash Metrics</h2>");
        List<String> deploymentNames = provider.listDeploymentNames();
        for (String deploymentName : deploymentNames) {
            showDataForDeployment(writer, deploymentName);
        }
    }

    private void showDataForDeployment(PrintWriter writer, String deploymentName) {
        writer.println("<h3>" + deploymentName + "</h3>");
        List<String> endpoints = provider.listEndpoints(deploymentName);
        writer.println("<ul>");
        for (String endpoint : endpoints) {
            Percentiles metrics = provider.getEndpointMetrics(deploymentName, endpoint);
            writer.println("<li>");
            writer.println(endpoint);
            writer.println(" : ");
            writer.println(metrics.getCount());
            writer.println(" : ");
            writer.println(metrics);  // FIXME a better layout
            writer.println("</li>");
        }
        writer.println("</ul>");
    }


}
