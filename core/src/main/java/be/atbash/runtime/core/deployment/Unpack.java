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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class Unpack {

    public static final String WEB_INF = File.separator + "WEB-INF";
    public static final String WEB_INF_CLASSES = File.separator + "WEB-INF" + File.separator + "classes";
    public static final String WEB_INF_LIB = File.separator + "WEB-INF" + File.separator + "lib";
    public static final String META_INF = File.separator + "META-INF";

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

    private void unpackArchive() throws IOException {
        JarInputStream jarInputStream = new JarInputStream(new FileInputStream(archiveFile));

        targetLocation.mkdirs();

        JarEntry jarEntry = jarInputStream.getNextJarEntry();

        while (jarEntry != null) {

            String filePath = targetLocation + File.separator + jarEntry.getName();
            if (!jarEntry.isDirectory()) {
                // if the entry is a file, extracts it
                extractFile(jarInputStream, filePath);
                keepTrackOfContent(filePath);
            } else {
                // if the entry is a directory, make the directory
                File dir = new File(filePath);
                dir.mkdirs();
            }
            jarInputStream.closeEntry();
            jarEntry = jarInputStream.getNextJarEntry();
        }
        jarInputStream.close();
    }

    private void keepTrackOfContent(String filePath) {
        int webInfindex = filePath.indexOf(WEB_INF);
        if (webInfindex > 0) {
            int index = filePath.indexOf(WEB_INF_CLASSES);

            if (index > 0) {
                archiveClassesFiles.add(filePath.substring(index + WEB_INF_CLASSES.length() + 1));

            } else {
                index = filePath.indexOf(WEB_INF_LIB);
                if (index > 0) {
                    archiveLibraryFiles.add(filePath.substring(index + WEB_INF_LIB.length() + 1));
                } else {
                    if (filePath.endsWith(".xml")) {
                        // Do we need other descriptior files.
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


    /**
     * Extracts a Jar entry (file entry)
     *
     * @param jarInputStream
     * @param filePath
     * @throws IOException
     */
    private static void extractFile(JarInputStream jarInputStream, String filePath) throws IOException {
        ensureDirectoryExists(filePath);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = jarInputStream.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

    private static void ensureDirectoryExists(String filePath) {
        File parentFile = new File(filePath).getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
    }

    public ArchiveContent processExpandedArchive() {
        // FIXME, this should not be needed to 'load' an application.
        // And does not take into account the Library and 'page' files.
        defineArchiveContent(targetLocation);
        return new ArchiveContent.ArchiveContentBuilder()
                .withClassesFiles(archiveClassesFiles).build();

    }

    private void defineArchiveContent(File directory) {
        // Get all files from a directory.
        File[] fList = directory.listFiles();
        if (fList != null) {
            for (File file : fList) {
                if (file.isFile()) {
                    Optional<String> content = stripLocation(file.getAbsolutePath());
                    content.ifPresent(archiveClassesFiles::add);
                } else if (file.isDirectory()) {
                    defineArchiveContent(file);
                }
            }
        }
    }

    private Optional<String> stripLocation(String filePath) {
        Optional<String> result = Optional.empty();
        int index = filePath.indexOf(Unpack.WEB_INF_CLASSES);
        if (index > 0) {
            result = Optional.of(filePath.substring(index + Unpack.WEB_INF_CLASSES.length() + 1));
        }
        return result;
    }
}
