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

import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.WebAppClassLoader;
import be.atbash.runtime.core.data.deployment.ArchiveContent;
import be.atbash.runtime.core.data.exception.UnexpectedException;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.core.data.util.ResourceReader;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class SpecificationChecker {

    private final ArchiveContent archiveContent;
    private final WebAppClassLoader classLoader;
    private final List<Sniffer> sniffers;
    private final List<Sniffer> triggeredSniffers;
    private final Set<Specification> specifications = new HashSet<>();

    public SpecificationChecker(ArchiveContent archiveContent, WebAppClassLoader classLoader, List<Sniffer> sniffers) {
        this.archiveContent = archiveContent;
        this.classLoader = classLoader;
        this.sniffers = new ArrayList<>(sniffers);
        triggeredSniffers = new ArrayList<>();
    }

    /**
     * Feed all classes of the archive itself to the Sniffers to determine what the Archive contains.
     * Note that the Classloader used for this is closed after performing this operation.
     */
    public void perform() {
        analyseClasses();
        if (!sniffers.isEmpty()) {
            analyseDescriptors();
        }
        classLoader.close();
    }

    private void analyseClasses() {
        try {
            for (String archiveClass : archiveContent.getArchiveClasses()) {

                Class<?> aClass = classLoader.loadClass(archiveClass);
                if (aClass == null) {
                    // We assume this is an exception and probably only happen with the TCK tests.
                    continue;
                }

                List<Sniffer> triggeredSniffers = sniffers.stream()
                        .filter(s -> s.triggered(aClass))
                        .collect(Collectors.toList());

                triggeredSniffers.forEach(s ->
                        specifications.addAll(Arrays.asList(s.detectedSpecifications())));

                updateSniffers(triggeredSniffers);

                if (sniffers.isEmpty()) {
                    break;
                    // No need to check the rest as all Sniffers are selected
                }
            }
        } catch (ClassNotFoundException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }
    }

    private void analyseDescriptors() {

        for (String descriptorFile : archiveContent.getDescriptorFiles()) {
            URL resource = classLoader.getResource(descriptorFile);
            String content;
            try {
                content = ResourceReader.readStringFromURL(resource);
            } catch (IOException e) {
                throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
            }


            List<Sniffer> triggeredSniffers = sniffers.stream()
                    .filter(s -> s.triggered(descriptorFile, content))
                    .collect(Collectors.toList());

            updateSniffers(triggeredSniffers);

            if (sniffers.isEmpty()) {
                break;
                // No need to check the rest as all Sniffers are selected
            }


        }

        triggeredSniffers.forEach(s ->
                specifications.addAll(Arrays.asList(s.detectedSpecifications())));

    }

    private void updateSniffers(List<Sniffer> triggeredSniffers) {
        for (Sniffer triggeredSniffer : triggeredSniffers) {
            if (!this.triggeredSniffers.contains(triggeredSniffer)) {
                this.triggeredSniffers.add(triggeredSniffer);
            }
            if (triggeredSniffer.isFastDetection()) {
                sniffers.remove(triggeredSniffer);
            }
        }
    }

    public Set<Specification> getSpecifications() {
        return specifications;
    }

    public List<Sniffer> getTriggeredSniffers() {
        return triggeredSniffers;
    }
}
