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
package be.atbash.runtime.monitor.core.util;

import jdk.jfr.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FlightRecorderUtil {

    private static final FlightRecorderUtil INSTANCE = new FlightRecorderUtil();

    private List<ValueDescriptor> fields;

    private FlightRecorderUtil() {

        fields = new ArrayList<>();
/*
        List<AnnotationElement> serverNameAnnotations = Collections.singletonList(new AnnotationElement(Label.class, "Server Name"));
        fields.add(new ValueDescriptor(String.class, "serverName", serverNameAnnotations));
        List<AnnotationElement> statusAnnotations = Collections.singletonList(new AnnotationElement(Label.class, "Status"));
        fields.add(new ValueDescriptor(String.class, "status", statusAnnotations));

 */
        List<AnnotationElement> messageAnnotations = Collections.singletonList(new AnnotationElement(Label.class, "Message"));
        fields.add(new ValueDescriptor(String.class, "message", messageAnnotations));
    }

    public void emitEvent(String module, String message) {

        String name = "be.atbash.runtime.event";
        String[] categories = {"Atbash", module};
        List<AnnotationElement> eventAnnotations = new ArrayList<>();

        eventAnnotations.add(new AnnotationElement(Name.class, name));
        eventAnnotations.add(new AnnotationElement(Label.class, "Atbash Runtime monitoring event"));
        eventAnnotations.add(new AnnotationElement(Description.class, "Atbash Runtime monitoring event"));
        eventAnnotations.add(new AnnotationElement(Category.class, categories));

        EventFactory eventFactory = EventFactory.create(eventAnnotations, fields);
        Event event = eventFactory.newEvent();
        event.set(0, message);

        event.commit();
    }

    public static FlightRecorderUtil getInstance() {
        return INSTANCE;
    }
}
