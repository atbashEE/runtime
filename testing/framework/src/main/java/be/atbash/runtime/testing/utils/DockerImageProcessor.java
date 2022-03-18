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
package be.atbash.runtime.testing.utils;

import be.atbash.runtime.testing.exception.DockerFileNotFound;
import be.atbash.runtime.testing.exception.UnexpectedException;
import be.atbash.runtime.testing.images.LoggableImageFromDockerFile;
import be.atbash.runtime.testing.model.RuntimeType;
import be.atbash.runtime.testing.model.ServerAdapterMetaData;
import org.assertj.core.api.Assertions;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class DockerImageProcessor {

    private static final Pattern IMAGE_PATTERN = Pattern.compile("atbash/(.*):(.*)");
    private static final Pattern IMAGE_PATTERN_LOCAL = Pattern.compile("runtime-main:(.*)");

    private DockerImageProcessor() {
    }

    /**
     * Returns the Docker image which will used in the test running Atbash Runtime.
     *
     * @param appFile The application file to include in the resulting Docker image
     * @return The docker image including the supplied appFile
     */
    public static ImageFromDockerfile getImage(ServerAdapterMetaData metaData, File appFile, String containerName) {

        String dockerFileContext = defineContent(metaData, containerName);

        try {
            Path tempDirWithPrefix = Files.createTempDirectory("atbash.test.");

            Path dockerPath = tempDirWithPrefix.resolve("Dockerfile");
            try (BufferedWriter writer = Files.newBufferedWriter(dockerPath)) {
                writer.write(dockerFileContext);
            }

            if (metaData.getRuntimeType() != RuntimeType.DEFAULT) {  // CUSTOM
                URI path = new File("./src/docker/" + metaData.getCustomImageName()).toURI();
                new FileCopyHelper(path.toURL(), tempDirWithPrefix).copyDependentFiles();

            }
            String name = "runtime";
            if (metaData.isTestApplication()) {
                Path source = appFile.toPath();
                Files.copy(source, tempDirWithPrefix.resolve("test.war"));
                name = appFile.getName();
            }

            return new LoggableImageFromDockerFile(containerName + "/" + name, metaData)
                    .withDockerfile(dockerPath);
        } catch (IOException e) {
            Assertions.fail(e.getMessage());
        }
        return null;
    }

    private static String defineContent(ServerAdapterMetaData metaData, String name) {
        StringBuilder result = new StringBuilder();
        String version = metaData.getJdkRuntime().getSuffix();

        switch (metaData.getRuntimeType()) {

            case DEFAULT:
                // FIXME What with the name of the image on DockerHub?
                result.append("FROM runtime-main").append(":").append(metaData.getRuntimeVersion()).append(version).append("\n");
                break;
            case CUSTOM:
                try {
                    File dockerFile = new File("./src/docker/" + metaData.getCustomImageName() + "/Dockerfile");
                    if (!dockerFile.exists() || !dockerFile.canRead()) {
                        throw new DockerFileNotFound("./src/docker/" + metaData.getCustomImageName() + "/Dockerfile");
                    }
                    String content = new String(Files.readAllBytes(Paths.get("./src/docker/" + metaData.getCustomImageName() + "/Dockerfile")));
                    result.append(updateTag(content, metaData));
                    result.append("\n");  // Make sure the ADD test.war ... is placed on a new line

                } catch (IOException e) {
                    throw new UnexpectedException("IOException during file read of 'src/docker/" + metaData.getCustomImageName() + "/DockerFile' ", e);
                }

                break;

            default:
                throw new IllegalStateException("Unexpected value: " + metaData.getRuntimeType());
        }

        if (metaData.isTestApplication()) {
            result.append("ADD test.war $DEPLOYMENT_DIR\n");
        }

        return result.toString();
    }

    // TODO Move this logic out of this class so that it can be tested.
    private static String updateTag(String content, ServerAdapterMetaData metaData) {
        return Arrays.stream(content.split("\n"))
                .map(l -> processLine(l, metaData))
                .collect(Collectors.joining("\n"));
    }

    private static String processLine(String line, ServerAdapterMetaData metaData) {
        String result = line.trim();
        if (result.toUpperCase(Locale.ENGLISH).startsWith("FROM")) {
            result = updateVersion(result, metaData);
        }
        return result;
    }

    private static String updateVersion(String fromLine, ServerAdapterMetaData metaData) {
        String[] parts = fromLine.split(" ");
        Matcher matcher = IMAGE_PATTERN.matcher(parts[1]);

        if (!matcher.matches()) {

            Matcher matcherLocal = IMAGE_PATTERN_LOCAL.matcher(parts[1]);

            if (!matcherLocal.matches()) {
                return fromLine;  // Keep the original content
                // Only perform the update of the Docker image tag when it is an Atbash official image.
            }
        }

        String newImage = "runtime-main" +
                ':' +
                metaData.getRuntimeVersion() +
                metaData.getJdkRuntime().getSuffix();

        parts[1] = newImage;

        return String.join(" ", parts);
    }
}
