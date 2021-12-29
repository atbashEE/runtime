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
package be.atbash.runtime.testing;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerNetwork;
import org.testcontainers.containers.GenericContainer;

import java.util.Map;

public final class DockerUtils {

    private DockerUtils() {
    }

    /**
     * genericCOntainer.getContainerIpAddress returns the Docker host address. This method return the
     * 'internal' ip address of the container.
     * @param dockerClient
     * @param containerId
     * @return
     */
    public static String getDockerContainerIP(DockerClient dockerClient, String containerId) {

        InspectContainerResponse resp = dockerClient.inspectContainerCmd(containerId).exec();
        Map<String, ContainerNetwork> networks = resp.getNetworkSettings().getNetworks();
        ContainerNetwork containerNetwork = networks.values().iterator().next();
        return containerNetwork.getIpAddress();

    }

    public static String getDockerContainerIP(GenericContainer<?> container) {
        return getDockerContainerIP(container.getDockerClient(), container.getContainerId());
    }
}
