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
package be.atbash.runtime.core.data.deployment;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class ArchiveContent {

    private List<String> classesFiles;
    private List<String> archiveClasses;

    public ArchiveContent(List<String> archiveFiles) {
        this.classesFiles = archiveFiles;
        determineClassNames();
    }

    private void determineClassNames() {
        archiveClasses = classesFiles.stream()
                // - 6 -> remove .class
                .filter(n -> n.endsWith(".class"))
                .map(n -> n.substring(0, n.length() - 6).replaceAll(File.separator, "."))
                .collect(Collectors.toList());
    }

    public List<String> getClassesFiles() {
        return classesFiles;
    }

    public List<String> getArchiveClasses() {
        return archiveClasses;
    }
}
