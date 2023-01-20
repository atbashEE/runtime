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
package be.atbash.runtime.jakarta.executable;

import jakarta.ws.rs.core.Application;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;


public final class JakartaSERunnerBuilder {

    private final JakartaRunnerData runnerData = new JakartaRunnerData();

    private JakartaSERunnerBuilder(Class<? extends Application> application) {
        runnerData.addResources(application);
    }

    private JakartaSERunnerBuilder(Class<?>... resourceClasses) {
        runnerData.addResources(resourceClasses);
    }

    public JakartaSERunnerBuilder withPort(int port) {
        validatePort(port);
        runnerData.setPort(port);
        return this;
    }

    private void validatePort(int port) {
        if (port < 0 || port > 65536) {
            throw new ParameterValidationException(String.format("The port value must be between 0 and 65536"));
        }
    }

    public JakartaSERunnerBuilder withHost(String host) {
        validateHost(host);

        runnerData.setHost(host);
        return this;
    }

    private void validateHost(String host) {
        InetAddress address;
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new ParameterValidationException("The host does not resolve or address is not a local address.");
        }
        if (!address.isLinkLocalAddress() && !address.isLoopbackAddress()) {
            throw new ParameterValidationException("The host does not resolve or address is not a local address.");
        }

    }

    public JakartaSERunnerBuilder addConfig(String key, String value) {
        runnerData.getApplicationData().put(key, value);
        return this;
    }

    public JakartaSERunnerBuilder addConfig(Map<String, String> values) {
        runnerData.getApplicationData().putAll(values);
        return this;
    }

    public JakartaSERunnerBuilder addCommandLineEntry(String value) {
        List<String> entries = new ArrayList<>();
        if (value.contains(" ")) {
            entries = Arrays.stream(value.split(" ")).collect(Collectors.toList());
        } else {
            entries.add(value);
        }
        runnerData.getCommandLineEntries().addAll(entries);

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
