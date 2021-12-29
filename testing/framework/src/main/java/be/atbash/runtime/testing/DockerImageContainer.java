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

import be.atbash.runtime.testing.utils.FileCopyHelper;
import org.assertj.core.api.Assertions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DockerImageContainer extends GenericContainer<DockerImageContainer> {

    public DockerImageContainer(String name) {

        super(getDockerImage(name));
        setNetwork(Network.SHARED);
    }

    private static ImageFromDockerfile getDockerImage(String name) {
        try {
            URI resource = new File(String.format("./src/docker/%s/Dockerfile", name)).toURI();

            Path dockerPath = Paths.get(resource);

            Path tempDirWithPrefix = Files.createTempDirectory("atbash.test.");
            Path dockerCopy = Files.copy(dockerPath, tempDirWithPrefix.resolve("Dockerfile"));

            resource = new File(String.format("./src/docker/%s", name)).toURI();
            // this will also copy the Dockerfile but we need dockerCopy for the new ImageFromDockerfile
            new FileCopyHelper(resource.toURL(), tempDirWithPrefix).copyDependentFiles();

            ImageFromDockerfile image = new ImageFromDockerfile("atbashtestcontainer/" + name)
                    .withDockerfile(dockerCopy);

            return image;
        } catch (IOException e) {
            Assertions.fail(e.getMessage());
        }
        return null; // The Assertions.fail will throw an exception so method can never return null.
    }
}
