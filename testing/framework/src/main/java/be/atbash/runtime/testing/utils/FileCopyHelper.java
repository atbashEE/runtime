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

import org.assertj.core.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class FileCopyHelper {

    private URL resource;
    private Path tempDir;

    private FileSystem fileSystem;

    /**
     * Construct an instance which copy the file or entire directory indicated by resource to the path.
     *
     * @param resource URL pointing to file or entire directory to copy. The jar locator is supported
     *                 through a custom FileSystem.
     * @param tempDir  Location to which file or directory contents needs to be copied.
     */
    public FileCopyHelper(URL resource, Path tempDir) {

        this.resource = resource;
        this.tempDir = tempDir;
    }

    /**
     * Copy the File to the directory
     *
     * @return Points to the copied Path of the file.
     */
    public Path copyToTemp() {
        Path result = null;
        Path path = definePath(resource);
        try {
            result = Files.copy(path, tempDir.resolve("Dockerfile"));
        } catch (IOException e) {
            Assertions.fail(e.getMessage());
        } finally {
            if (fileSystem != null) {
                try {
                    fileSystem.close();
                } catch (IOException e) {
                    Assertions.fail(e.getMessage());
                }
            }
        }
        return result;
    }

    private Path definePath(URL resource) {
        Path result = null;
        try {
            String uriString = resource.toURI().toString();
            if (uriString.contains("!")) {
                int idx = uriString.indexOf('!');
                // 4 -> jar:file:
                URI uri = new URI("jar", uriString.substring(4, idx), null);
                Map<String, String> env = new HashMap<>();
                env.put("create", "true");

                fileSystem = FileSystems.newFileSystem(uri, env);
                result = fileSystem.getPath("/").resolve(uriString.substring(idx + 2));
            } else {
                result = Paths.get(resource.toURI());
            }
        } catch (URISyntaxException | IOException e) {
            Assertions.fail(e.getMessage());
        }
        return result;
    }

    /**
     * Copy all files within the directory to the temp dir.
     */
    public void copyDependentFiles() {

        try {
            try (Stream<Path> pathStream = Files.find(Paths.get(resource.toURI()),
                    Integer.MAX_VALUE,
                    (filePath, fileAttr) -> fileAttr.isRegularFile()
                            && !filePath.getFileName().endsWith("Dockerfile"))) {
                // For each of the found files, copy it to the temporary directory.
                pathStream.forEach(p -> copyToTemp(p, tempDir));
            }
        } catch (IOException | URISyntaxException e) {
            Assertions.fail(e.getMessage());
        }

    }

    private void copyToTemp(Path path, Path tempDirWithPrefix) {

        Path targetPath = determineTargetPath(path, tempDirWithPrefix);
        File parentDirectory = targetPath.getParent().toFile();
        if (!parentDirectory.exists()) {
            boolean success = parentDirectory.mkdirs();
            if (!success) {
                Assertions.fail(String.format("Unable to create directory %s", parentDirectory));
            }
        }
        try {
            Files.copy(path, targetPath, REPLACE_EXISTING);
        } catch (IOException e) {
            Assertions.fail(e.getMessage());
        }
    }

    private Path determineTargetPath(Path path, Path tempDirWithPrefix) {
        int length = resource.getPath().length();
        String endPath = path.toString().substring(length);

        return tempDirWithPrefix.resolve(endPath);
    }
}
