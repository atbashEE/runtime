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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArchiveContentTest {

    @Test
    void getArchiveClasses() {
        List<String> classesFiles = new ArrayList<>();
        classesFiles.add("be/atbash/runtime/core/Test.class");
        ArchiveContent content = new ArchiveContent.ArchiveContentBuilder()
                .withClassesFiles(classesFiles)
                .build();

        assertThat(content.isEmpty()).isFalse();
        assertThat(content.getArchiveClasses()).containsExactly("be.atbash.runtime.core.Test");
    }

    @Test
    void isEmpty() {
        ArchiveContent content = new ArchiveContent.ArchiveContentBuilder().build();
        assertThat(content.isEmpty()).isTrue();
    }

    @Test
    void isEmpty_withEmptyLists() {
        ArchiveContent content = new ArchiveContent.ArchiveContentBuilder()
                .withClassesFiles(Collections.emptyList())
                .withPagesFiles(Collections.emptyList())
                .withDescriptorFiles(Collections.emptyList())
                .withLibraryFiles(Collections.emptyList())
                .build();
        assertThat(content.isEmpty()).isTrue();
    }

    @Test
    void isEmpty_withPagesFiles() {
        ArchiveContent content = new ArchiveContent.ArchiveContentBuilder()
                .withPagesFiles(List.of("/something.html"))
                .build();
        assertThat(content.isEmpty()).isFalse();
    }

    @Test
    void isEmpty_withLibraryFiles() {
        ArchiveContent content = new ArchiveContent.ArchiveContentBuilder()
                .withLibraryFiles(List.of("atbash-util.jar"))
                .build();
        assertThat(content.isEmpty()).isFalse();
    }

    @Test
    void isEmpty_withDescriptorFiles() {
        ArchiveContent content = new ArchiveContent.ArchiveContentBuilder()
                .withDescriptorFiles(List.of("web.xml"))
                .build();
        assertThat(content.isEmpty()).isFalse();
    }
}