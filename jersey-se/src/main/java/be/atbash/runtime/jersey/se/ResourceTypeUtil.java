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
package be.atbash.runtime.jersey.se;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.ext.Provider;

public final class ResourceTypeUtil {

    private ResourceTypeUtil() {
    }

    public static ResourceType determineType(Class<?> someClass) {
        ResourceType result = ResourceType.CLASS;
        if (Application.class.isAssignableFrom(someClass)) {
            result = ResourceType.APPLICATION;
        } else {
            if (someClass.getAnnotation(Path.class) != null ||
                    someClass.getAnnotation(Provider.class) != null) {
                result = ResourceType.RESOURCE;
            }
        }
        return result;
    }
}
