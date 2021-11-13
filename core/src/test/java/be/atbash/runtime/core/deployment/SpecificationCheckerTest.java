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
package be.atbash.runtime.core.deployment;

import be.atbash.runtime.core.data.WebAppClassLoader;
import be.atbash.runtime.core.data.deployment.ArchiveContent;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.core.deployment.sniffer.CollectingSniffer;
import be.atbash.runtime.core.deployment.sniffer.NeverTriggeredSniffer;
import be.atbash.runtime.core.deployment.sniffer.SingleTriggeredSniffer;
import be.atbash.runtime.core.deployment.sniffer.TestSniffer;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SpecificationCheckerTest {

    @Test
    void perform() throws IOException {

        File root = new File(".", "../demo/demo-rest/target/demo-rest").getCanonicalFile();

        List<String> files = new ArrayList<>();
        defineArchiveContent(root, files);
        ArchiveContent archive = new ArchiveContent(files);

        WebAppClassLoader classLoader = new WebAppClassLoader(root, SpecificationCheckerTest.class.getClassLoader());
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
                assertThat(((TestSniffer)sniffer).getSeenClasses()).hasSize(1);
            }
            if (sniffer instanceof NeverTriggeredSniffer) {
                assertThat(((TestSniffer)sniffer).getSeenClasses().size()).isGreaterThan(1);
            }
        }
    }

    public void defineArchiveContent(File directory, List<String> files) {
        // Get all files from a directory.
        File[] fList = directory.listFiles();
        if (fList != null) {
            for (File file : fList) {
                if (file.isFile()) {
                    Optional<String> content = stripLocation(file.getAbsolutePath());
                    content.ifPresent(files::add);
                } else if (file.isDirectory()) {
                    defineArchiveContent(file, files);
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