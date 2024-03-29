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
package be.atbash.runtime.core.deployment;

import be.atbash.runtime.core.data.deployment.ArchiveContent;
import be.atbash.runtime.core.data.exception.UnexpectedException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class Unpack {

    public static final String WEB_INF = File.separator + "WEB-INF";
    public static final String WEB_INF_CLASSES = File.separator + "WEB-INF" + File.separator + "classes";
    public static final String WEB_INF_LIB = File.separator + "WEB-INF" + File.separator + "lib";
    public static final String META_INF = "META-INF";
    public static final String META_INF_MAVEN = META_INF + File.separator + "maven";

    private static final List<String> IGNORED_LIBRARIES = Arrays.asList("arquillian-core.jar", "arquillian-junit.jar", "arquillian-testng.jar");

    /**
     * Size of the buffer to read/write data
     */
    private static final int BUFFER_SIZE = 4096;
    private File archiveFile;
    private final File targetLocation;

    private final List<String> archiveClassesFiles = new ArrayList<>();
    private final List<String> archiveLibraryFiles = new ArrayList<>();
    private final List<String> archiveDescriptorFiles = new ArrayList<>();
    private final List<String> archivePageFiles = new ArrayList<>();  // like html, jsp, ...

    public Unpack(File archiveFile, File targetLocation) {
        this.archiveFile = archiveFile;
        this.targetLocation = targetLocation;

    }

    public Unpack(File targetLocation) {
        this.targetLocation = targetLocation;
    }

    public ArchiveContent handleArchiveFile() {
        try {
            unpackArchive();

        } catch (IOException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }

        scanLibraryFiles();

        ArchiveContent content = new ArchiveContent.ArchiveContentBuilder()
                .withClassesFiles(archiveClassesFiles)
                .withLibraryFiles(archiveLibraryFiles)
                .withDescriptorFiles(archiveDescriptorFiles)
                .withPagesFiles(archivePageFiles)
                .build();

        if (content.isEmpty()) {
            return null;
        }
        return content;
    }

    private void scanLibraryFiles() {

        File parentLib = new File(targetLocation, "WEB-INF/lib");
        for (String archiveLibraryFile : archiveLibraryFiles) {
            if (IGNORED_LIBRARIES.contains(archiveLibraryFile)) {
                // No need to scan these libraries.
                continue;
            }
            File jarFile = new File(parentLib, archiveLibraryFile);
            try {
                scanArchive(jarFile);
            } catch (IOException e) {
                throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
            }
        }
    }

    private void unpackArchive() throws IOException {
        try (JarInputStream jarInputStream = new JarInputStream(new FileInputStream(archiveFile))) {

            targetLocation.mkdirs();

            JarEntry jarEntry = jarInputStream.getNextJarEntry();

            while (jarEntry != null) {

                String filePath = targetLocation + File.separator + jarEntry.getName();
                if (!jarEntry.isDirectory()) {
                    // if the entry is a file, extracts it
                    extractFile(jarInputStream, filePath);
                    keepTrackOfContent(filePath);
                    duplicateMetaInfEntriesIfNeeded(jarEntry, filePath);
                } else {
                    // if the entry is a directory, make the directory
                    File dir = new File(filePath);
                    dir.mkdirs();
                }
                jarInputStream.closeEntry();
                jarEntry = jarInputStream.getNextJarEntry();
            }
        }
    }

    private void duplicateMetaInfEntriesIfNeeded(JarEntry jarEntry, String originalFile) throws IOException {
        String jarEntryName = jarEntry.getName();
        if (jarEntryName.startsWith(META_INF) && !jarEntryName.startsWith(META_INF_MAVEN)) {

            // META-INF entry directory within root, duplicate it under classpath -> WEB-INF/classes
            String filePath = targetLocation + File.separator + "WEB-INF" + File.separator + "classes" + File.separator + jarEntryName;
            ensureDirectoryExists(filePath);
            Files.copy(Path.of(originalFile), Path.of(filePath));

            keepTrackOfContent(filePath);
        }
    }

    private void keepTrackOfContent(String filePath) {
        int webInfindex = filePath.indexOf(WEB_INF);
        if (webInfindex > 0) {
            int index = filePath.indexOf(WEB_INF_CLASSES);

            if (index > 0 && filePath.endsWith(".class")) {
                archiveClassesFiles.add(filePath.substring(index + WEB_INF_CLASSES.length() + 1));

            } else {
                index = filePath.indexOf(WEB_INF_LIB);
                if (index > 0) {
                    archiveLibraryFiles.add(filePath.substring(index + WEB_INF_LIB.length() + 1));
                } else {
                    if (filePath.endsWith(".xml") || filePath.endsWith(".properties")) {
                        // Do we need other descriptor files?
                        archiveDescriptorFiles.add(filePath.substring(webInfindex + WEB_INF.length() + 1));
                    }
                }
            }

        } else {
            if (!filePath.contains(META_INF)) {
                archivePageFiles.add(filePath);
            }
        }
    }

    private void scanArchive(File jarFile) throws IOException {
        try (JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarFile))) {

            JarEntry jarEntry = jarInputStream.getNextJarEntry();

            while (jarEntry != null) {

                // Don't need the full path here as we are not expanding it, just keep track of it.
                if (!jarEntry.isDirectory()) {
                    String filePath = jarFile.getName() + "!/" + jarEntry.getName();
                    // if the entry is a file, keep the name
                    keepTrackOfJarContent(filePath);
                }
                jarInputStream.closeEntry();
                jarEntry = jarInputStream.getNextJarEntry();
            }
        }
    }

    private void keepTrackOfJarContent(String filePath) {
        int metaInfindex = filePath.indexOf(META_INF);
        if (metaInfindex > 0) {
            int index = filePath.indexOf(File.separator + META_INF_MAVEN);

            if (index == -1 && (filePath.endsWith(".xml") || filePath.endsWith(".properties"))) {
                archiveDescriptorFiles.add(filePath);

            }
        } else {
            if (filePath.endsWith(".class")) {

                archiveClassesFiles.add(filePath);
            }
        }
    }

    /**
     * Extracts a Jar entry (file entry)
     *
     * @param jarInputStream
     * @param filePath
     * @throws IOException
     */
    private static void extractFile(JarInputStream jarInputStream, String filePath) throws IOException {
        ensureDirectoryExists(filePath);
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
            byte[] bytesIn = new byte[BUFFER_SIZE];
            int read;
            while ((read = jarInputStream.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }

    private static void ensureDirectoryExists(String filePath) {
        File parentFile = new File(filePath).getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
    }
}
