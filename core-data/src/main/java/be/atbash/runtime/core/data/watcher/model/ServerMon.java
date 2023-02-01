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
package be.atbash.runtime.core.data.watcher.model;

import java.util.List;

public class ServerMon implements ServerMonMBean {

    private String version;
    private final long startOfServer;
    private List<String> startedModules;
    private String mode;

    public ServerMon(long startOfServer) {
        this.startOfServer = startOfServer;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public List<String> getStartedModules() {
        return startedModules;
    }

    public void setStartedModules(List<String> startedModules) {
        this.startedModules = startedModules;
    }

    @Override
    public Long uptime() {
        return (System.currentTimeMillis() - startOfServer) / 1000;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    @Override
    public String getMode() {
        return mode;
    }
}
