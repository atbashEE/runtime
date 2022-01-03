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
package be.atbash.runtime.core.data;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.fail;

class WebAppClassLoaderTest {

    @Test
    public void loadClass() throws IOException, ClassNotFoundException {

        File rootDirectory = new File("../demo/demo-servlet/target/demo-servlet");

        WebAppClassLoader loader = new WebAppClassLoader(rootDirectory.getCanonicalFile(), Collections.emptyList(), this.getClass().getClassLoader());
        Class<?> aClass = loader.loadClass("be.atbash.runtime.demo.servlet.HelloServlet");
        Assertions.assertThat(aClass).isNotNull(); //And no exception is thrown is also important
    }

    @Test
    public void loadDescriptor() throws IOException, ClassNotFoundException {

        File rootDirectory = new File("../demo/demo-servlet/target/demo-servlet");

        WebAppClassLoader loader = new WebAppClassLoader(rootDirectory.getCanonicalFile(), Collections.emptyList(), this.getClass().getClassLoader());
        URL descriptorURL = loader.findResource("web.xml");
        Assertions.assertThat(descriptorURL).isNotNull();
        String content = readStringFromURL(descriptorURL);
        Assertions.assertThat(content).contains("<web-app");
        Assertions.assertThat(content).contains("<servlet-mapping>");
    }

    private String readStringFromURL(URL requestURL) throws IOException {
        try (Scanner scanner = new Scanner(requestURL.openStream(),
                StandardCharsets.UTF_8.toString())) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

}