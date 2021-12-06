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
package be.atbash.runtime.core.data.parameter;

import picocli.CommandLine;

public class BasicRemoteCLIParameters {

    @CommandLine.Option(names = {"-h", "--host"}, description = "Host name or IP address of the machine running the Atbash runtime in Domain mode.")
    private String host = "localhost";

    @CommandLine.Option(names = {"-p", "--port"}, description = "Port number assigned the process running the Atbash runtime in Domain mode.")
    private int port = 8080;

    @CommandLine.Option(names = {"-f", "--format"}, description = "Format output of the Remote CLI commands.  Support values are TEXT and JSON.")
    private RemoteCLIOutputFormat format = RemoteCLIOutputFormat.TEXT;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public RemoteCLIOutputFormat getFormat() {
        return format;
    }

    public void setFormat(RemoteCLIOutputFormat format) {
        this.format = format;
    }
}
