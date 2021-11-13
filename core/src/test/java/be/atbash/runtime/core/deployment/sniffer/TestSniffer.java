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
package be.atbash.runtime.core.deployment.sniffer;

import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;

import java.util.ArrayList;
import java.util.List;

public abstract class TestSniffer implements Sniffer {

    private List<Class<?>> seenClasses = new ArrayList<>();

    protected void addClass(Class<?> clazz) {
        seenClasses.add(clazz);
    }

    public List<Class<?>> getSeenClasses() {
        return seenClasses;
    }

    @Override
    public Specification[] detectedSpecifications() {
        return new Specification[0];
    }

    @Override
    public boolean triggered(String descriptorName, String content) {
        return false;
    }

}
