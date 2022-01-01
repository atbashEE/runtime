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

import be.atbash.runtime.testing.config.Config;
import be.atbash.runtime.testing.model.ServerAdapterMetaData;
import be.atbash.runtime.testing.utils.DockerImageProcessor;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.Network;

public class AtbashContainer extends AbstractContainer<AtbashContainer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AtbashContainer.class);

    private final ServerAdapterMetaData adapterMetaData;
    private final boolean liveLogging;

    public AtbashContainer(ServerAdapterMetaData adapterMetaData, boolean liveLogging) {
        super(DockerImageProcessor.getImage(adapterMetaData, findAppFile(adapterMetaData.isTestApplication(), LOGGER), "atbashcontainer"));
        this.adapterMetaData = adapterMetaData;
        this.liveLogging = liveLogging;
        setNetwork(Network.SHARED);
    }

    @Override
    protected void configure() {
        super.configure();

        containerConfiguration(LOGGER, liveLogging);

        int appStartTimeout = Config.getAppStartTimeout();
        if (adapterMetaData.isDebugMode()) {
            // In debug mode, wait 2 mins so that developer has time to attach debugger to container process.
            appStartTimeout = 120;
        }
        if (adapterMetaData.isTestStartupFailure()) {
            withLogEntry(appStartTimeout);
        } else {
            withReadinessPath("/health", appStartTimeout);
        }

        withEnv("ATBASH_ARGS", String.join(" ", adapterMetaData.getStartupParameters()));
        if (System.getProperty("be.atbash.runtime.container.mutable") != null) {
            withEnv("STATELESS", "false");
        }

        if (adapterMetaData.isDebugMode()) {
            withEnv("JVM_ARGS", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005");
            addFixedExposedPort(5005, 5005);
        }
    }

    @Override
    public String getApplicationPort() {
        return "8080";
    }

    @Override
    public int getMappedApplicationPort() {
        return getMappedPort(8080);
    }

    public InspectContainerResponse.ContainerState getStatus() {
        return dockerClient.inspectContainerCmd(getContainerId()).exec().getState();
    }

    //  we need some helper methods to make this usage easier.
    /* TODO future version
    private String executeInContainer(String... execArgs) {
        String result = "<fail>";
        try {
            ExecResult commandResult = execInContainer(execArgs);
            if (commandResult.getExitCode() != 0) {
                Assertions.fail(commandResult.getStdout() + "\n" + commandResult.getStderr());
            } else {

                result = commandResult.getStdout();

            }
        } catch (IOException | InterruptedException e) {
            Assertions.fail(e.getMessage());
        }

        return result;
    }

     */

    @Override
    protected void waitUntilContainerStarted() {
        try {
            super.waitUntilContainerStarted();
        } catch (ContainerLaunchException e) {
            // If container stopped before waitStrategy was satisfied, maybe it was ok because
            // we are testing if the startup on the runtime fails.
            if (!adapterMetaData.isTestStartupFailure()) {
                throw e;
            }
        }
    }

    /*
     * A Transferable which keeps data in memory. Use it only for small transfers to the container.
     *  TODO Future version
    private static class StringTransferable implements Transferable {
        private String content;
        private byte[] bytes;

        StringTransferable(String content) {
            this.content = content;
            bytes = content.getBytes();
        }


        public long getSize() {
            return this.bytes.length;
        }

        public String getDescription() {
            return "String: " + StringUtils.abbreviate(content, 100);
        }

        public byte[] getBytes() {
            return this.bytes;
        }
    }

     */
}
