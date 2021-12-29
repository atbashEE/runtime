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

import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Some code on top of the GenericContainer to get the correct Adapter (responsible for defining the correct Docker image)
 * and having some general useful methods.
 *
 * @param <T>
 */
public abstract class AbstractContainer<T extends GenericContainer<T>> extends GenericContainer<T> {

    public AbstractContainer(final Future<String> image) {
        super(image);
    }

    public String getContainerIP() {
        return DockerUtils.getDockerContainerIP(getDockerClient(), getContainerId());
    }

    public abstract String getApplicationPort();

    public abstract int getMappedApplicationPort();

    /**
     * Value returned by `InetAddress.getLocalHost().getHostName()` within the container.
     *
     * @return Hostname of the container.
     */
    public String getHostName() {
        return getContainerId().substring(0, 12);
    }

    public byte[] getContainerFileContent(String path) {

        return copyFileFromContainer(path, inputStream -> {
            ByteArrayOutputStream content = new ByteArrayOutputStream();

            IOUtils.copy(inputStream, content);
            return content.toByteArray();

        });
    }

    /**
     * Returns reference to the project build artifact (.war or .ear) to include in the Docker Image.
     *
     * @param testApplicationRequired is application expected? When no build artifact found but application expected an exception is thrown.
     * @param logger                  Logger
     * @return File Reference to found artifact or null when no artifact and no application expected.
     */
    protected static File findAppFile(boolean testApplicationRequired, Logger logger) {
        if (!testApplicationRequired) {
            return null;
        }
        // Find a .war file in the target/ directories
        Set<File> matches = new HashSet<>(findAppFiles("target"));
        if (matches.size() == 0) {
            throw new IllegalStateException("No .war or .ear files found in target / output folders.");
        }
        if (matches.size() > 1) {
            throw new IllegalStateException("Found multiple application files in target output folders: " + matches +
                    " Expecting exactly 1 application file to be found.");
        }
        File appFile = matches.iterator().next();
        logger.info("Found application file at: " + appFile.getAbsolutePath());
        return appFile;
    }

    /**
     * Configure the timeout check using the readiness path.
     *
     * @param readinessUrl   The relative URL for the check
     * @param timeoutSeconds The timeout
     */
    protected void withReadinessPath(String readinessUrl, int timeoutSeconds) {
        Objects.requireNonNull(readinessUrl);
        readinessUrl = buildPath(readinessUrl);
        waitingFor(Wait.forHttp(readinessUrl)
                .withStartupTimeout(Duration.ofSeconds(timeoutSeconds)));
    }

    /**
     * Configure the timeout check using the log entry check.
     *
     * @param timeoutSeconds The timeout
     */
    protected void withLogEntry(int timeoutSeconds) {
        waitingFor(Wait.forLogMessage(".*CLI-10[7|8]:.*", 1)
                .withStartupTimeout(Duration.ofSeconds(timeoutSeconds)));
    }

    /**
     * Configure container properties like exposed ports and logging.
     *
     * @param logger         The Logger for live logging.
     * @param liveLogging
     */
    protected void containerConfiguration(Logger logger, boolean liveLogging) {
        addExposedPorts(8080);  // FIXME
        if (liveLogging) {
            withLogConsumer(new Slf4jLogConsumer(logger));
        }

    }

    /**
     * Find all .war and .ear files in a directory and subdirectories.
     *
     * @param path The top level directory to start the search
     * @return The set of files found matching the file type.
     */
    private static Set<File> findAppFiles(String path) {
        File dir = new File(path);
        if (dir.exists() && dir.isDirectory()) {
            try {
                return Files.walk(dir.toPath())
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().toLowerCase().endsWith(".war"))
                        .map(Path::toFile)
                        .collect(Collectors.toSet());
            } catch (IOException ignore) {
            }
        }
        return Collections.emptySet();
    }

    private static String buildPath(String firstPart, String... moreParts) {
        StringBuilder result = new StringBuilder(firstPart.startsWith("/") ? firstPart : '/' + firstPart);
        if (moreParts != null && moreParts.length > 0) {
            for (String part : moreParts) {
                if (result.toString().endsWith("/") && part.startsWith("/")) {
                    result.append(part.substring(1));
                } else if (result.toString().endsWith("/") || part.startsWith("/")) {
                    result.append(part);
                } else {
                    result.append("/").append(part);
                }
            }
        }
        return result.toString();
    }
}
