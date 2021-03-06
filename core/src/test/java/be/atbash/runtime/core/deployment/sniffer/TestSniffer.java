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
package be.atbash.runtime.core.deployment.sniffer;

import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class TestSniffer implements Sniffer {

    private final List<Class<?>> seenClasses = new ArrayList<>();
    private final List<String> seenDescriptors = new ArrayList<>();
    protected Specification[] specifications;

    public TestSniffer() {
        this.specifications = new Specification[]{};
    }

    protected void addClass(Class<?> clazz) {
        seenClasses.add(clazz);
    }

    protected void addDescriptor(String descriptorFile) {
        seenDescriptors.add(descriptorFile);
    }

    public List<Class<?>> getSeenClasses() {
        return seenClasses;
    }

    public List<String> getSeenDescriptors() {
        return seenDescriptors;
    }

    @Override
    public Specification[] detectedSpecifications() {
        return specifications;
    }

    @Override
    public boolean triggered(String descriptorName, String content) {
        return false;
    }

    @Override
    public Map<String, String> deploymentData() {
        return Collections.EMPTY_MAP;
    }
}
