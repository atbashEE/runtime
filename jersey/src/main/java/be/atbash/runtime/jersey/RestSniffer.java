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
package be.atbash.runtime.jersey;

import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import jakarta.ws.rs.ApplicationPath;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

public class RestSniffer implements Sniffer {

    private final List<Class<?>> applicationClasses = new ArrayList<>();
    private final List<Class<?>> resourceClasses = new ArrayList<>();

    private final List<Class<?>> providerClasses = new ArrayList<>();

    @Override
    public Specification[] detectedSpecifications() {
        return new Specification[]{Specification.REST};
    }

    @Override
    @SuppressWarnings("squid:S1872")
    public boolean triggered(Class<?> aClass) {
        for (Annotation annotation : aClass.getAnnotations()) {
            if ("jakarta.ws.rs.Path".equals(annotation.annotationType().getName())) {
                resourceClasses.add(aClass);
            }
            if ("jakarta.ws.rs.ext.Provider".equals(annotation.annotationType().getName())) {
                providerClasses.add(aClass);
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

    @Override
    public Map<String, String> deploymentData() {
        Map<String, String> result = new HashMap<>();
        result.put(JerseyModuleConstant.CLASS_NAMES, String.join(",", resourceClassNames()));
        result.put(JerseyModuleConstant.PACKAGE_NAMES, String.join(",", determinePackages()));
        result.put(JerseyModuleConstant.APPLICATION_PATH, findApplicationPath());

        return result;
    }

    private Set<String> determinePackages() {
        // We use a set to have unique package names.
        List<Class<?>> allClasses = new ArrayList<>();
        allClasses.addAll(resourceClasses);
        allClasses.addAll(providerClasses);
        return allClasses
                .stream()
                .map(Class::getPackageName)
                .collect(Collectors.toSet());
    }

    private List<String> resourceClassNames() {
        return resourceClasses
                .stream()
                .map(Class::getName)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("squid:S1872")
    private String findApplicationPath() {

        String result = null;
        // FIXME should only be 0 or 1.
        if (!applicationClasses.isEmpty()) {
            Optional<Annotation> annotation = Arrays.stream(applicationClasses.get(0).getAnnotations())
                    .filter(a -> "jakarta.ws.rs.ApplicationPath".equals(a.annotationType().getName()))
                    .findAny();
            if (annotation.isPresent()) {
                ApplicationPath applicationPath = (ApplicationPath) annotation.get();
                result = applicationPath.value();
            }
        }
        return result;
    }

}
