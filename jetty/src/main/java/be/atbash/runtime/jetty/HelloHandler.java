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

import org.eclipse.jetty.server.handler.AbstractHandler;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;

public class HelloHandler extends AbstractHandler {

    final String _greeting;

    final String _body;

    public HelloHandler() {
        _greeting = "Hello World";
        _body = null;
    }

    public HelloHandler(String greeting) {
        _greeting = greeting;
        _body = null;
    }

    public HelloHandler(String greeting, String body) {
        _greeting = greeting;
        _body = body;
    }

    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        response.getWriter().println("<h1>" + _greeting + "</h1>");
        if (_body != null) response.getWriter().println(_body);
    }
}
