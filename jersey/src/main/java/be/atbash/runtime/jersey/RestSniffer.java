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
package be.atbash.runtime.jersey;

import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RestSniffer implements Sniffer {

    private List<Class<?>> applicationClasses = new ArrayList<>();
    private List<Class<?>> resourceClasses = new ArrayList<>();

    @Override
    public Specification[] detectedSpecifications() {
        return new  Specification[]{Specification.REST};
    }

    @Override
    public boolean triggered(Class<?> aClass) {
        for (Annotation annotation : aClass.getAnnotations()) {
            if ("jakarta.ws.rs.Path".equals(annotation.annotationType().getName())) {
                resourceClasses.add(aClass);
            }
            if ("jakarta.ws.rs.ApplicationPath".equals(annotation.annotationType().getName())) {
                // FIXME, this is not the only way to define the base URI
                applicationClasses.add(aClass);
            }
        }
        return !applicationClasses.isEmpty();

    }

    @Override
    public boolean triggered(String descriptorName, String content) {
        return false;
    }

    @Override
    public boolean isFastDetection() {
        return false;
    }

    public List<Class<?>> getApplicationClasses() {
        return applicationClasses;
    }

    public List<Class<?>> getResourceClasses() {
        return resourceClasses;
    }
}
