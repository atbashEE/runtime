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
package be.atbash.runtime.core.deployment.monitor;

import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.core.deployment.sniffer.SingleTriggeredSniffer;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationInfoTest {

    @Test
    void testConstructor() {
        List<Sniffer> sniffers = Collections.singletonList(new SingleTriggeredSniffer());
        Set<Specification> specifications = Collections.singleton(Specification.SERVLET);

        ArchiveDeployment deployment = new ArchiveDeployment("locationNotImportant"
                , "name", specifications, sniffers, "/root", new HashMap<>());

        ApplicationInfo info = ApplicationInfo.createFor(deployment);
        assertThat(info.getSniffers()).containsExactly("SingleTriggeredSniffer");
        assertThat(info.getSpecifications()).containsExactly(Specification.SERVLET);
    }

    @Test
    void testToString() {
        List<Sniffer> sniffers = Collections.singletonList(new SingleTriggeredSniffer());
        Set<Specification> specifications = Collections.singleton(Specification.SERVLET);

        ArchiveDeployment deployment = new ArchiveDeployment("locationNotImportant"
                , "name", specifications, sniffers, "/root", new HashMap<>());

        ApplicationInfo info = ApplicationInfo.createFor(deployment);
        assertThat(info.toString()).isEqualTo("context root for application /root, detected specifications SERVLET, triggered sniffers SingleTriggeredSniffer");

    }

}