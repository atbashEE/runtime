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
package be.atbash.runtime.testing.server;

import be.atbash.runtime.testing.config.Config;
import be.atbash.runtime.testing.model.JDKRuntime;
import be.atbash.runtime.testing.model.RuntimeType;
import be.atbash.runtime.testing.model.ServerAdapterMetaData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServerAdapterMetaDataTest {

    @AfterEach
    public void cleanup() {
        System.clearProperty("atbash.runtime.version");
    }

    @Test
    void parse_basic() {
        ServerAdapterMetaData metaData = ServerAdapterMetaData.parse("default");
        assertThat(metaData).isNotNull();
        assertThat(metaData.getRuntimeType()).isEqualTo(RuntimeType.DEFAULT);
        assertThat(metaData.getJdkRuntime()).isEqualTo(Config.getJDKRuntime());
    }

    @Test
    void parse_full() {
        ServerAdapterMetaData metaData = ServerAdapterMetaData.parse("default-0.3-jdk17");
        assertThat(metaData).isNotNull();
        assertThat(metaData.getRuntimeType()).isEqualTo(RuntimeType.DEFAULT);
        assertThat(metaData.getRuntimeVersion()).isEqualTo("0.3");
        assertThat(metaData.getJdkRuntime()).isEqualTo(JDKRuntime.JDK17);
    }

    @Test
    void parse_jdkonly() {
        System.setProperty("atbash.runtime.version", "0.3.1.2");
        ServerAdapterMetaData metaData = ServerAdapterMetaData.parse("--jdk17");
        assertThat(metaData).isNotNull();
        assertThat(metaData.getRuntimeType()).isEqualTo(RuntimeType.DEFAULT);
        assertThat(metaData.getRuntimeVersion()).isEqualTo("0.3.1.2");
        assertThat(metaData.getJdkRuntime()).isEqualTo(JDKRuntime.JDK17);
    }

    @Test
    void parse_customName() {
        System.setProperty("atbash.runtime.version", "0.3");
        ServerAdapterMetaData metaData = ServerAdapterMetaData.parse("domain");
        assertThat(metaData).isNotNull();
        assertThat(metaData.getRuntimeType()).isEqualTo(RuntimeType.CUSTOM);
        assertThat(metaData.getCustomImageName()).isEqualTo("domain");
        assertThat(metaData.getRuntimeVersion()).isEqualTo("0.3");
        assertThat(metaData.getJdkRuntime()).isEqualTo(Config.getJDKRuntime());
    }

    @Test
    void parse_custom() {
        ServerAdapterMetaData metaData = ServerAdapterMetaData.parse("custom-5.193");
        assertThat(metaData).isNotNull();
        assertThat(metaData.getRuntimeType()).isEqualTo(RuntimeType.CUSTOM);
        assertThat(metaData.getCustomImageName()).isEqualTo("custom");
        assertThat(metaData.getRuntimeVersion()).isEqualTo("5.193");
        assertThat(metaData.getJdkRuntime()).isEqualTo(Config.getJDKRuntime());
    }

    @Test
    void parse_snapshot() {
        ServerAdapterMetaData metaData = ServerAdapterMetaData.parse("default-0.4-SNAPSHOT-jdk17");
        assertThat(metaData).isNotNull();
        assertThat(metaData.getRuntimeType()).isEqualTo(RuntimeType.DEFAULT);
        assertThat(metaData.getRuntimeVersion()).isEqualTo("0.4-SNAPSHOT");
        assertThat(metaData.getJdkRuntime()).isEqualTo(JDKRuntime.JDK17);
    }

    @Test
    void parse_rc() {
        ServerAdapterMetaData metaData = ServerAdapterMetaData.parse("custom-0.4-RC1");
        assertThat(metaData).isNotNull();
        assertThat(metaData.getRuntimeType()).isEqualTo(RuntimeType.CUSTOM);
        assertThat(metaData.getCustomImageName()).isEqualTo("custom");
        assertThat(metaData.getRuntimeVersion()).isEqualTo("0.4-RC1");
        assertThat(metaData.getJdkRuntime()).isEqualTo(JDKRuntime.JDK11);
    }

    @Test
    void parse_useDefaultJDKIfUnknown() {
        ServerAdapterMetaData metaData = ServerAdapterMetaData.parse("micro-0.4-something");
        assertThat(metaData).isNotNull();
        assertThat(metaData.getRuntimeType()).isEqualTo(RuntimeType.CUSTOM);
        assertThat(metaData.getCustomImageName()).isEqualTo("micro");
        assertThat(metaData.getRuntimeVersion()).isEqualTo("0.4");
        assertThat(metaData.getJdkRuntime()).isEqualTo(Config.getJDKRuntime());
    }

}