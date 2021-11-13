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
package be.atbash.runtime.jetty;

import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Optional;

public class ServletSniffer implements Sniffer {

    @Override
    public Specification[] detectedSpecifications() {
        return new  Specification[]{Specification.SERVLET, Specification.HTML};
    }

    @Override
    public boolean triggered(Class<?> aClass) {
        Optional<Annotation> WebServletAnnotation = Arrays.stream(aClass.getAnnotations())
                .filter(a -> "jakarta.servlet.annotation.WebServlet".equals(a.annotationType().getName()))
                .findAny();
        return WebServletAnnotation.isPresent();

    }

    @Override
    public boolean triggered(String descriptorName, String content) {
        return false;
    }

    @Override
    public boolean isFastDetection() {
        return true;
    }
}
