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

import be.atbash.runtime.core.data.WebAppClassLoader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RestSnifferTest {

    @Test
    void deploymentData() throws IOException, ClassNotFoundException {

        File rootDirectory = new File("../demo/demo-rest/target/demo-rest");
        WebAppClassLoader loader = new WebAppClassLoader(rootDirectory.getCanonicalFile(), Collections.emptyList(), this.getClass().getClassLoader());

        RestSniffer sniffer = new RestSniffer();

        sniffer.triggered(loader.loadClass("be.atbash.runtime.demo.rest.resources.HelloResource"));
        sniffer.triggered(loader.loadClass("be.atbash.runtime.demo.rest.resources.RequestResource"));
        sniffer.triggered(loader.loadClass("be.atbash.runtime.demo.rest.provider.DemoContainerRequestFilter"));
        sniffer.triggered(loader.loadClass("be.atbash.runtime.demo.rest.JaxRsActivator"));

        Map<String, String> data = sniffer.deploymentData();

        assertThat(data).hasSize(3);
        assertThat(data.keySet()).contains(JerseyModuleConstant.APPLICATION_PATH, JerseyModuleConstant.PACKAGE_NAMES, JerseyModuleConstant.CLASS_NAMES);
        assertThat(data.get(JerseyModuleConstant.APPLICATION_PATH)).isEqualTo("/rest");
        assertThat(data.get(JerseyModuleConstant.PACKAGE_NAMES)).isEqualTo("be.atbash.runtime.demo.rest.resources,be.atbash.runtime.demo.rest.provider");
        assertThat(data.get(JerseyModuleConstant.CLASS_NAMES)).contains("be.atbash.runtime.demo.rest.resources.HelloResource");
        assertThat(data.get(JerseyModuleConstant.CLASS_NAMES)).contains("be.atbash.runtime.demo.rest.resources.RequestResource");
        /// FIXME we need a 3th JAX-RS resource in another package so that we can test the concatenation.
    }
}