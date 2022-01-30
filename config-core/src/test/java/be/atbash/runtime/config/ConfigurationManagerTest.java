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
package be.atbash.runtime.config;

import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.config.Config;
import be.atbash.runtime.core.data.config.Modules;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static be.atbash.runtime.config.RuntimeConfigConstants.CONFIG_FILE;

class ConfigurationManagerTest {


    @Test
    void setCommand() {
        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        Config config = new Config();
        config.setModules(new Modules());
        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .setConfig(config)
                .build();

        ConfigurationManager manager = new ConfigurationManager(runtimeConfiguration);
        List<String> result = manager.setCommand(new String[]{"module.key.parts=value"});
        Assertions.assertThat(result.isEmpty()).isTrue();

        File configFile = new File(configDirectory, CONFIG_FILE);
        Assertions.assertThat(configFile).exists();

        Assertions.assertThat(configFile).hasContent("{\"modules\":{\"configuration\":{\"module\":{\"key.parts\":\"value\"}}}}");

    }

    @Test
    void setCommand_multiple() {
        File configDirectory = new File("./target/testDirectory2");
        configDirectory.mkdirs();

        Config config = new Config();
        config.setModules(new Modules());
        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .setConfig(config)
                .build();

        ConfigurationManager manager = new ConfigurationManager(runtimeConfiguration);
        List<String> result = manager.setCommand(new String[]{"module.key.parts=value", "mpconfig.validation.disable=true"});
        Assertions.assertThat(result.isEmpty()).isTrue();

        File configFile = new File(configDirectory, CONFIG_FILE);
        Assertions.assertThat(configFile).exists();

        Assertions.assertThat(configFile).hasContent("{\"modules\":{\"configuration\":{\"module\":{\"key.parts\":\"value\"},\"mpconfig\":{\"validation.disable\":\"true\"}}}}");

    }

    @Test
    void setCommand_missingValue() {
        File configDirectory = new File("./target/testDirectory3");
        configDirectory.mkdirs();

        Config config = new Config();
        config.setModules(new Modules());
        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .setConfig(config)
                .build();

        ConfigurationManager manager = new ConfigurationManager(runtimeConfiguration);
        List<String> result = manager.setCommand(new String[]{"module.key.parts"});
        Assertions.assertThat(result.isEmpty()).isFalse();
        Assertions.assertThat(result).containsExactly("CONFIG-101: Option must be 2 parts separated by =, received 'module.key.parts'");


        File configFile = new File(configDirectory, CONFIG_FILE);
        Assertions.assertThat(configFile).doesNotExist();

    }

    @Test
    void setCommand_incorrectKey() {
        File configDirectory = new File("./target/testDirectory3");
        configDirectory.mkdirs();

        Config config = new Config();
        config.setModules(new Modules());
        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .setConfig(config)
                .build();

        ConfigurationManager manager = new ConfigurationManager(runtimeConfiguration);
        List<String> result = manager.setCommand(new String[]{"key=value"});
        Assertions.assertThat(result.isEmpty()).isFalse();
        Assertions.assertThat(result).containsExactly("CONFIG-102: Option key must be '.' separated value, received 'key'");


        File configFile = new File(configDirectory, CONFIG_FILE);
        Assertions.assertThat(configFile).doesNotExist();

    }
}