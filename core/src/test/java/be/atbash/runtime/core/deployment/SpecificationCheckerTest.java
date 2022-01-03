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

import be.atbash.runtime.core.data.WebAppClassLoader;
import be.atbash.runtime.core.data.deployment.ArchiveContent;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.core.deployment.sniffer.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SpecificationCheckerTest {

    @Test
    void perform() throws IOException {

        File root = new File(".", "../demo/demo-rest/target/demo-rest").getCanonicalFile();

        List<String> classFiles = new ArrayList<>();
        List<String> descriptorFiles = new ArrayList<>();
        defineArchiveContent(root, classFiles, descriptorFiles);
        ArchiveContent archive = new ArchiveContent.ArchiveContentBuilder()
                .withClassesFiles(classFiles)
                .withDescriptorFiles(descriptorFiles)
                .build();

        WebAppClassLoader classLoader = new WebAppClassLoader(root, Collections.emptyList(), SpecificationCheckerTest.class.getClassLoader());
        List<Sniffer> sniffers = List.of(new SingleTriggeredSniffer()
                , new CollectingSniffer()
                , new NeverTriggeredSniffer());
        SpecificationChecker checker = new SpecificationChecker(archive, classLoader, sniffers);
        checker.perform();

        List<Sniffer> triggeredSniffers = checker.getTriggeredSniffers();

        assertThat(triggeredSniffers).hasSize(2);
        for (int i = 0; i < 2; i++) {
            Sniffer sniffer = triggeredSniffers.get(i);
            if (sniffer instanceof SingleTriggeredSniffer) {
                assertThat(((TestSniffer) sniffer).getSeenClasses()).hasSize(1);
            }
            if (sniffer instanceof NeverTriggeredSniffer) {
                assertThat(((TestSniffer) sniffer).getSeenClasses().size()).isGreaterThan(1);
            }
        }
    }

    @Test
    void perform_withDescriptor() throws IOException {

        File root = new File(".", "../demo/demo-servlet/target/demo-servlet").getCanonicalFile();

        List<String> classFiles = new ArrayList<>();
        List<String> descriptorFiles = new ArrayList<>();
        defineArchiveContent(root, classFiles, descriptorFiles);
        ArchiveContent archive = new ArchiveContent.ArchiveContentBuilder()
                .withClassesFiles(classFiles)
                .withDescriptorFiles(descriptorFiles)
                .build();

        WebAppClassLoader classLoader = new WebAppClassLoader(root, Collections.emptyList(), SpecificationCheckerTest.class.getClassLoader());
        List<Sniffer> sniffers = List.of(new SingleTriggeredSniffer()
                , new CollectingSniffer()
                , new DescriptorSniffer()
                , new NeverTriggeredSniffer());
        SpecificationChecker checker = new SpecificationChecker(archive, classLoader, sniffers);
        checker.perform();

        List<Sniffer> triggeredSniffers = checker.getTriggeredSniffers();

        assertThat(triggeredSniffers).hasSize(3);
        for (int i = 0; i < 2; i++) {
            Sniffer sniffer = triggeredSniffers.get(i);
            if (sniffer instanceof SingleTriggeredSniffer) {
                assertThat(((TestSniffer) sniffer).getSeenClasses()).hasSize(1);
            }
            if (sniffer instanceof NeverTriggeredSniffer) {
                assertThat(((TestSniffer) sniffer).getSeenClasses().size()).isGreaterThan(1);
            }
            if (sniffer instanceof DescriptorSniffer) {
                assertThat(((TestSniffer) sniffer).getSeenDescriptors()).hasSize(1);
            }
        }
    }

    public void defineArchiveContent(File directory, List<String> classFiles, List<String> descriptorFiles) {
        // Get all files from a directory.
        File[] fList = directory.listFiles();
        if (fList != null) {
            for (File file : fList) {
                if (file.isFile()) {
                    Optional<String> content = stripLocation(file.getAbsolutePath());
                    content.filter(name -> name.endsWith(".class")).ifPresent(classFiles::add);
                    content.filter(name -> name.endsWith(".xml")).ifPresent(descriptorFiles::add);
                } else if (file.isDirectory()) {
                    defineArchiveContent(file, classFiles, descriptorFiles);
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
        if (result.isEmpty()) {
            index = filePath.indexOf(Unpack.WEB_INF);
            if (index > 0) {
                result = Optional.of(filePath.substring(index + Unpack.WEB_INF.length() + 1));
            }
        }
        return result;
    }

}