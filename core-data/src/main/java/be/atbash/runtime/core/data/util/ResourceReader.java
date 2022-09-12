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
package be.atbash.runtime.core.data.util;

import be.atbash.util.resource.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;

/**
 * Centralised access to the ResourceUtil class from utils-se library.
 */
public final class ResourceReader {

    private static final ResourceUtil resourceUtil = ResourceUtil.getInstance();

    private ResourceReader() {
    }

    public static String readResource(String location) throws IOException {
        return resourceUtil.getContent(location);
    }

    public static InputStream getStream(String location) throws IOException {
        return resourceUtil.getStream(location);
    }

    public static List<URI> getResources(String location) {
        return resourceUtil.getResources(location);
    }

    public static boolean existsResource(String location) {
        return resourceUtil.resourceExists(location);
    }

    public static String readStringFromURL(URL requestURL) throws IOException {
        return resourceUtil.getContent(requestURL.toExternalForm());
    }

}
