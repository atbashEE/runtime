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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServletSnifferTest {

    @Test
    void triggered() {
        boolean triggered = new ServletSniffer().triggered("web.xml", "<web-app><servlet-mapping><mapping>value</mapping></servlet-mapping></web-app>");
        assertThat(triggered).isTrue();
    }

    @Test
    void triggered_notWebXml() {
        boolean triggered = new ServletSniffer().triggered("some.xml", "<web-app><servlet-mapping><mapping>value</mapping></servlet-mapping></web-app>");
        assertThat(triggered).isFalse();
    }

    @Test
    void triggered_wrong_content() {
        boolean triggered = new ServletSniffer().triggered("web.xml", "<web-app><servlet><name>value</name></servlet></web-app>");
        assertThat(triggered).isFalse();
    }

    @Test
    void triggered_asComment() {
        boolean triggered = new ServletSniffer().triggered("web.xml", "<web-app><servlet><name>value</name><!-- servlet-mapping --></servlet></web-app>");
        assertThat(triggered).isFalse();
    }

}