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
package be.atbash.runtime.jersey.util;

import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.jersey.RestSniffer;
import jakarta.ws.rs.ApplicationPath;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ResourcePathUtil {

    private static final ResourcePathUtil INSTANCE = new ResourcePathUtil();

    private ResourcePathUtil() {
    }

    public List<String> determinePackages(RestSniffer Sniffer) {
        return Sniffer.getResourceClasses()
                .stream()
                .map(Class::getPackageName)
                .collect(Collectors.toList());
    }

    public String findApplicationPath(ArchiveDeployment deployment) {
        RestSniffer restSniffer = (RestSniffer) deployment.getSniffers().stream()
                .filter(s -> RestSniffer.class.equals(s.getClass()))
                .findAny()
                .orElseGet(() -> null);

        String result = null;
        if (restSniffer != null) {
            List<Class<?>> applicationClasses = restSniffer.getApplicationClasses();
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
        }
        return result;
    }

    public static ResourcePathUtil getInstance() {
        return INSTANCE;
    }
}
