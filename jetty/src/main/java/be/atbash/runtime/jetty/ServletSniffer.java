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
package be.atbash.runtime.jetty;

import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.exception.UnexpectedException;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class ServletSniffer implements Sniffer {

    @Override
    public Specification[] detectedSpecifications() {
        return new Specification[]{Specification.SERVLET, Specification.HTML};
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
        boolean result = false;
        if ("web.xml".equals(descriptorName)) {
            result = checkForServletMappings(content);
        }
        if (descriptorName.endsWith("!/META-INF/web-fragment.xml")) {
            result = checkForServletMappings(content);
        }

        return result;
    }

    private boolean checkForServletMappings(String content) {
        boolean result = false;
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        try {
            XMLEventReader reader = xmlInputFactory.createXMLEventReader(new StringReader(content));
            while (reader.hasNext()) {
                XMLEvent nextEvent = reader.nextEvent();
                if (nextEvent.isStartElement()) {
                    StartElement startElement = nextEvent.asStartElement();
                    if (startElement.getName().getLocalPart().equals("servlet-mapping")) {
                        result = true;
                        break;
                    }

                }
            }
        } catch (XMLStreamException  e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }
        return result;
    }

    @Override
    public boolean isFastDetection() {
        return true;
    }

    @Override
    public Map<String, String> deploymentData() {
        return Collections.EMPTY_MAP;
    }

}
