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
package be.atbash.runtime.core.data.deployment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ArchiveContent {

    private final List<String> classesFiles;
    private final List<String> libraryFiles;
    private final List<String> descriptorFiles;
    private final List<String> pagesFiles;

    private List<String> archiveClasses;

    private ArchiveContent(List<String> classesFiles, List<String> libraryFiles, List<String> descriptorFiles, List<String> pagesFiles) {
        this.classesFiles = classesFiles;
        this.libraryFiles = libraryFiles;
        this.descriptorFiles = descriptorFiles;
        this.pagesFiles = pagesFiles;

        determineClassNames();
    }

    private void determineClassNames() {
        archiveClasses = classesFiles.stream()
                .filter(n -> n.endsWith(".class"))  // To be on the safe side
                // - 6 -> remove .class
                .map(n -> n.substring(0, n.length() - 6).replaceAll(File.separator, "."))
                .collect(Collectors.toList());
    }

    public List<String> getLibraryFiles() {
        return libraryFiles;
    }

    public List<String> getPagesFiles() {
        return pagesFiles;
    }

    public List<String> getArchiveClasses() {
        return archiveClasses;
    }

    public List<String> getDescriptorFiles() {
        return descriptorFiles;
    }

    public boolean isEmpty() {
        return classesFiles.isEmpty() && libraryFiles.isEmpty() && pagesFiles.isEmpty() && descriptorFiles.isEmpty();
    }

    public static class ArchiveContentBuilder {
        private List<String> classesFiles;
        private List<String> libraryFiles;
        private List<String> descriptorFiles;
        private List<String> pagesFiles;

        public ArchiveContentBuilder withClassesFiles(List<String> classesFiles) {
            this.classesFiles = classesFiles;
            return this;
        }

        public ArchiveContentBuilder withLibraryFiles(List<String> libraryFiles) {
            this.libraryFiles = libraryFiles;
            return this;
        }

        public ArchiveContentBuilder withPagesFiles(List<String> pagesFiles) {
            this.pagesFiles = pagesFiles;
            return this;
        }

        public ArchiveContentBuilder withDescriptorFiles(List<String> descriptorFiles) {
            this.descriptorFiles = descriptorFiles;
            return this;
        }

        public ArchiveContent build() {
            if (classesFiles == null) {
                classesFiles = Collections.emptyList();
            }
            if (libraryFiles == null) {
                libraryFiles = Collections.emptyList();
            }
            if (descriptorFiles == null) {
                descriptorFiles = Collections.emptyList();
            }
            if (pagesFiles == null) {
                pagesFiles = Collections.emptyList();
            }
            return new ArchiveContent(classesFiles, libraryFiles, descriptorFiles, pagesFiles);
        }
    }
}
