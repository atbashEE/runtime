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
package be.atbash.runtime.core.data.deployment;

import java.util.HashMap;
import java.util.List;

public class ApplicationExecution extends AbstractDeployment {

    private final List<Class<?>> resources;

    private int port;

    private String host;

    public ApplicationExecution(List<Class<?>> resources, String root) {
        super("Jakarta Core Profile application", root, new HashMap<>());
        this.resources = resources;
        deploymentPhase = DeploymentPhase.VERIFIED;
    }

    public List<Class<?>> getResources() {
        return resources;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

}
