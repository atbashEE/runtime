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
package be.atbash.runtime.testing;

import be.atbash.runtime.cli.command.AbstractRemoteAtbashCommand;
import be.atbash.runtime.core.data.parameter.BasicRemoteCLIParameters;
import be.atbash.runtime.testing.jupiter.ShowLogWhenFailedExceptionHandler;
import be.atbash.util.TestReflectionUtils;
import com.fasterxml.jackson.core.util.JacksonFeature;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import org.junit.Assert;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit.jupiter.Container;

/**
 * Parent class for all the Atbash Runtime Container tests. The tests also needs to be annotated with @AtbashContainerTest.
 */
public class AbstractAtbashTest {

    /**
     * The test will inject a reference to the Docker container running the Atbash Runtime instance under test.
     * Use the methods to interact with the container.
     */
    @Container
    public static AtbashContainer atbash;

    @RegisterExtension
    private final ShowLogWhenFailedExceptionHandler showLogExceptionHandler = new ShowLogWhenFailedExceptionHandler();

    private Client client;


    // Start of JAX--RS Rest client related methods.
    private void defineClient() {
        if (client == null) {
            client = ClientBuilder.newClient();
            client.register(JacksonFeature.class);
        }
    }
    /**
     * Returns a JAX-RS WebTarget for the Runtime instance running on the Container specified in the parameter.
     * @param container Container reference of the application we want to access.
     * @return JAX-RS WebTarget to the Runtime instance.
     */
    protected WebTarget getClientWebTarget(AbstractContainer<?> container) {
        defineClient();
        // getContainerIpAddress and getMappedApplicationPort since we run this locally
        return client.target("http://" + container.getHost() + ":" + container.getMappedApplicationPort());
    }

    /**
     * Returns a JAX-RS WebTarget to the application running on the Container specified in the parameter.
     * @param container Container reference of the application we want to access.
     * @return JAX-RS WebTarget to the test application.
     */
    protected WebTarget getClientWebTargetApplication(AbstractContainer<?> container) {
        defineClient();
        // getContainerIpAddress and getMappedApplicationPort since we run this locally
        return client.target("http://" + container.getHost() + ":" + container.getMappedApplicationPort() + "/test");
    }

    /**
     * Configure
     * @param command
     */
    protected void configureRemoteCommand(AbstractRemoteAtbashCommand command) {
        BasicRemoteCLIParameters basicRemoteCLIParameters = new BasicRemoteCLIParameters();
        basicRemoteCLIParameters.setPort(atbash.getMappedApplicationPort());
        try {
            TestReflectionUtils.setFieldValue(command, "basicRemoteCLIParameters", basicRemoteCLIParameters);
        } catch (NoSuchFieldException e) {
            Assert.fail(e.getMessage());
        }

    }
}
