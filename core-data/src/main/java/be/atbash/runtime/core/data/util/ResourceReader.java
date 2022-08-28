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

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public final class ResourceReader {

    private ResourceReader() {
    }

    public static String readResource(String location) throws IOException {
        InputStream stream = ResourceReader.class.getResourceAsStream(location);
        if (stream == null) {
            throw new FileNotFoundException("Unable to read resource " + location);
        }
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int length; (length = stream.read(buffer)) != -1; ) {
            result.write(buffer, 0, length);
        }
        stream.close();
        return result.toString(StandardCharsets.UTF_8);
    }

    public static boolean existsResource(String location)  {
        URL resourceURL = ResourceReader.class.getResource(location);
        return resourceURL != null;
    }

    public static String readStringFromURL(URL requestURL) throws IOException {
        try (Scanner scanner = new Scanner(requestURL.openStream(),
                StandardCharsets.UTF_8)) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

}
