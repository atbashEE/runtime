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

import be.atbash.runtime.metrics.collector.PercentileValue;
import be.atbash.runtime.metrics.collector.Percentiles;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.assertj.core.api.Assertions;
import org.eclipse.jetty.server.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ExtendWith(MockitoExtension.class)
class PrometheusMetricsHandlerTest {

    private static Pattern REGEX_COUNT = Pattern.compile("application_response_time_seconds_count\\{application=\"([^-\\s]+)\",endpoint=\"([^-\\s]+)\"} ([0-9]+)");
    private static Pattern REGEX_QUANTILE = Pattern.compile("application_response_time_seconds\\{application=\"([^-\\s]+)\",endpoint=\"([^-\\s]+)\",quantile=\"([.0-9]+)\"} ([0-9]+)");

    @Mock
    private Request baseRequestMock;

    @Mock
    private HttpServletResponse responseMock;

    @Test
    void handle() throws ServletException, IOException {

        PrometheusMetricsHandler metricsHandler = new PrometheusMetricsHandler();
        TestDataProvider dataProvider = new TestDataProvider();
        metricsHandler.setProvider(dataProvider);

        dataProvider.addData("app1", "endpoint1", percentiles(0));

        StringWriter data = new StringWriter();
        PrintWriter writer = new PrintWriter(data);
        Mockito.when(responseMock.getWriter()).thenReturn(writer);
        metricsHandler.handle("/metrics", baseRequestMock, null, responseMock);

        String[] lines = data.toString().split("\n");
        Assertions.assertThat(lines).hasSize(12);
        Assertions.assertThat(lines[0]).isEqualTo("# TYPE application_response_time_seconds summary");
        Assertions.assertThat(lines[1]).isEqualTo("# HELP application_response_time_seconds Server response time");

        Matcher matcher = REGEX_COUNT.matcher(lines[2]);
        Assertions.assertThat(matcher.matches()).isTrue();
        Assertions.assertThat(matcher.group(1)).isEqualTo("app1");
        Assertions.assertThat(matcher.group(2)).isEqualTo("endpoint1");
        Assertions.assertThat(matcher.group(3)).isEqualTo("100");

        int idx = 3;
        for (PercentileValue percentileValue : PercentileValue.values()) {

            Matcher matcherQuantile = REGEX_QUANTILE.matcher(lines[idx++]);
            Assertions.assertThat(matcherQuantile.matches()).isTrue();
            Assertions.assertThat(matcherQuantile.group(1)).isEqualTo("app1");
            Assertions.assertThat(matcherQuantile.group(2)).isEqualTo("endpoint1");
            Assertions.assertThat(matcherQuantile.group(3)).isEqualTo(Double.toString(percentileValue.getValue() / 100.0));
            Assertions.assertThat(matcherQuantile.group(4)).isEqualTo(Integer.toString(percentileValue.getValue()));

        }

    }

    @Test
    void handle_2endpoints() throws ServletException, IOException {

        PrometheusMetricsHandler metricsHandler = new PrometheusMetricsHandler();
        TestDataProvider dataProvider = new TestDataProvider();
        metricsHandler.setProvider(dataProvider);

        dataProvider.addData("app1", "endpoint1", percentiles(0));
        dataProvider.addData("app1", "endpoint2", percentiles(50));

        StringWriter data = new StringWriter();
        PrintWriter writer = new PrintWriter(data);
        Mockito.when(responseMock.getWriter()).thenReturn(writer);
        metricsHandler.handle("/metrics", baseRequestMock, null, responseMock);

        String[] lines = data.toString().split("\n");
        Assertions.assertThat(lines).hasSize(22);
        Assertions.assertThat(lines[0]).isEqualTo("# TYPE application_response_time_seconds summary");
        Assertions.assertThat(lines[1]).isEqualTo("# HELP application_response_time_seconds Server response time");

        Matcher matcher = REGEX_COUNT.matcher(lines[2]);
        Assertions.assertThat(matcher.matches()).isTrue();
        Assertions.assertThat(matcher.group(1)).isEqualTo("app1");
        Assertions.assertThat(matcher.group(2)).isEqualTo("endpoint1");
        Assertions.assertThat(matcher.group(3)).isEqualTo("100");

        int idx = 3;
        for (PercentileValue percentileValue : PercentileValue.values()) {

            Matcher matcherQuantile = REGEX_QUANTILE.matcher(lines[idx++]);
            Assertions.assertThat(matcherQuantile.matches()).isTrue();
            Assertions.assertThat(matcherQuantile.group(1)).isEqualTo("app1");
            Assertions.assertThat(matcherQuantile.group(2)).isEqualTo("endpoint1");
            Assertions.assertThat(matcherQuantile.group(3)).isEqualTo(Double.toString(percentileValue.getValue() / 100.0));
            Assertions.assertThat(matcherQuantile.group(4)).isEqualTo(Integer.toString(percentileValue.getValue()));

        }

        // endpoint 2
        matcher = REGEX_COUNT.matcher(lines[idx++]);
        Assertions.assertThat(matcher.matches()).isTrue();
        Assertions.assertThat(matcher.group(1)).isEqualTo("app1");
        Assertions.assertThat(matcher.group(2)).isEqualTo("endpoint2");
        Assertions.assertThat(matcher.group(3)).isEqualTo("100");

        for (PercentileValue percentileValue : PercentileValue.values()) {
            Matcher matcherQuantile = REGEX_QUANTILE.matcher(lines[idx++]);
            Assertions.assertThat(matcherQuantile.matches()).isTrue();
            Assertions.assertThat(matcherQuantile.group(1)).isEqualTo("app1");
            Assertions.assertThat(matcherQuantile.group(2)).isEqualTo("endpoint2");
            Assertions.assertThat(matcherQuantile.group(3)).isEqualTo(Double.toString(percentileValue.getValue() / 100.0));
            Assertions.assertThat(matcherQuantile.group(4)).isEqualTo(Integer.toString(percentileValue.getValue() + 50));

        }

    }

    @Test
    void handle_2Apps() throws ServletException, IOException {

        PrometheusMetricsHandler metricsHandler = new PrometheusMetricsHandler();
        TestDataProvider dataProvider = new TestDataProvider();
        metricsHandler.setProvider(dataProvider);

        dataProvider.addData("app1", "endpoint1", percentiles(0));
        dataProvider.addData("app2", "endpoint3", percentiles(50));

        StringWriter data = new StringWriter();
        PrintWriter writer = new PrintWriter(data);
        Mockito.when(responseMock.getWriter()).thenReturn(writer);
        metricsHandler.handle("/metrics", baseRequestMock, null, responseMock);

        String[] lines = data.toString().split("\n");
        Assertions.assertThat(lines).hasSize(22);
        Assertions.assertThat(lines[0]).isEqualTo("# TYPE application_response_time_seconds summary");
        Assertions.assertThat(lines[1]).isEqualTo("# HELP application_response_time_seconds Server response time");

        Matcher matcher = REGEX_COUNT.matcher(lines[2]);
        Assertions.assertThat(matcher.matches()).isTrue();
        Assertions.assertThat(matcher.group(1)).isEqualTo("app1");
        Assertions.assertThat(matcher.group(2)).isEqualTo("endpoint1");
        Assertions.assertThat(matcher.group(3)).isEqualTo("100");

        int idx = 3;
        for (PercentileValue percentileValue : PercentileValue.values()) {

            Matcher matcherQuantile = REGEX_QUANTILE.matcher(lines[idx++]);
            Assertions.assertThat(matcherQuantile.matches()).isTrue();
            Assertions.assertThat(matcherQuantile.group(1)).isEqualTo("app1");
            Assertions.assertThat(matcherQuantile.group(2)).isEqualTo("endpoint1");
            Assertions.assertThat(matcherQuantile.group(3)).isEqualTo(Double.toString(percentileValue.getValue() / 100.0));
            Assertions.assertThat(matcherQuantile.group(4)).isEqualTo(Integer.toString(percentileValue.getValue()));

        }

        // app 2
        matcher = REGEX_COUNT.matcher(lines[idx++]);
        Assertions.assertThat(matcher.matches()).isTrue();
        Assertions.assertThat(matcher.group(1)).isEqualTo("app2");
        Assertions.assertThat(matcher.group(2)).isEqualTo("endpoint3");
        Assertions.assertThat(matcher.group(3)).isEqualTo("100");

        for (PercentileValue percentileValue : PercentileValue.values()) {
            Matcher matcherQuantile = REGEX_QUANTILE.matcher(lines[idx++]);
            Assertions.assertThat(matcherQuantile.matches()).isTrue();
            Assertions.assertThat(matcherQuantile.group(1)).isEqualTo("app2");
            Assertions.assertThat(matcherQuantile.group(2)).isEqualTo("endpoint3");
            Assertions.assertThat(matcherQuantile.group(3)).isEqualTo(Double.toString(percentileValue.getValue() / 100.0));
            Assertions.assertThat(matcherQuantile.group(4)).isEqualTo(Integer.toString(percentileValue.getValue() + 50));

        }

    }

    private static Percentiles percentiles(long shift) {
        long[] data = new long[100];
        for (int i = 0; i < 100; i++) {
            data[i] = i + 1 + shift;
        }
        return new Percentiles(data, 100);
    }
}