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

import jakarta.ws.rs.core.Application;

import java.util.ServiceLoader;

public final class JakartaSERunnerBuilder {

    private final JakartaRunnerData runnerData = new JakartaRunnerData();

    private JakartaSERunnerBuilder(Class<? extends Application> application) {
        runnerData.addResources(application);
    }

    private JakartaSERunnerBuilder(Class<?>... resourceClasses) {
        runnerData.addResources(resourceClasses);
    }

    public JakartaSERunnerBuilder withPort(int port) {
        runnerData.setPort(port);
        return this;
    }

    public JakartaSERunnerBuilder withHost(String host) {
        runnerData.setHost(host);
        return this;
    }

    public JakartaSERunnerBuilder addConfig(String key, String value) {
        runnerData.getApplicationData().put(key, value);
        return this;
    }

    public void run() {
        getRunner().start(runnerData);
    }

    private JakartaRunner getRunner() {
        ServiceLoader<JakartaRunner> loader = ServiceLoader.load(JakartaRunner.class);
        return loader.findFirst().orElseThrow(JakartaRunnerNotFoundException::new);
    }

    public static JakartaSERunnerBuilder newBuilder(Class<? extends Application> application) {
        return new JakartaSERunnerBuilder(application);
    }

    public static JakartaSERunnerBuilder newBuilder(Class<?>... resourceClasses) {
        return new JakartaSERunnerBuilder(resourceClasses);
    }
}
