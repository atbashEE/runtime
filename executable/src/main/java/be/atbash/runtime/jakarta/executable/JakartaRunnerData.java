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
package be.atbash.runtime.jakarta.executable;

import java.util.*;

public class JakartaRunnerData {

    private final List<Class<?>> resources = new ArrayList<>();
    private int port = 8080;

    private String host = "localhost";

    private String root = "/";

    private Map<String, String> applicationData = new HashMap<>();
    private List<String> commandLineEntries = new ArrayList<>();

    public List<Class<?>> getResources() {
        return resources;
    }

    public void addResources(Class<?>... resources) {
        this.resources.addAll(Arrays.asList(resources));
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

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = Objects.requireNonNull(root);
        sanitizeRoot();
    }

    private void sanitizeRoot() {
        if (!root.startsWith("/")) {
            root = "/" + root;
        }
        if (root.endsWith("/")) {
            root = root.substring(0, root.length() - 1);
        }
    }

    public Map<String, String> getApplicationData() {
        return applicationData;
    }

    public void setApplicationData(Map<String, String> applicationData) {
        this.applicationData = applicationData;
    }

    public List<String> getCommandLineEntries() {
        return commandLineEntries;
    }

    public void setCommandLineEntries(List<String> commandLineEntries) {
        this.commandLineEntries = commandLineEntries;
    }
}
