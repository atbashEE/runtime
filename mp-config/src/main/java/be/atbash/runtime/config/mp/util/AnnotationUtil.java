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
package be.atbash.runtime.config.mp.util;

import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.InjectionPoint;
import org.eclipse.microprofile.config.inject.ConfigProperties;

import java.util.Optional;

import static org.eclipse.microprofile.config.inject.ConfigProperties.UNCONFIGURED_PREFIX;

/**
 * Based on code from SmallRye Config.
 */
public final class AnnotationUtil {
    private AnnotationUtil() {
    }

    public static ConfigProperties getConfigPropertiesAnnotation(InjectionPoint injectionPoint) {
        Annotated annotated = injectionPoint.getAnnotated();
        if (annotated != null) {
            return annotated.getAnnotation(ConfigProperties.class);
        }
        return null;
    }

    public static Optional<String> parsePrefix(ConfigProperties annotation) {
        if (annotation == null) {
            return Optional.empty();
        }
        String value = annotation.prefix();
        if (value == null || value.equals(UNCONFIGURED_PREFIX)) {
            return Optional.empty();
        }
        if (value.isEmpty()) {
            return Optional.of("");
        }
        return Optional.of(value + ".");
    }
}
