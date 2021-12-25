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
package be.atbash.runtime.core;


import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.exception.AtbashStartupAbortException;
import be.atbash.runtime.core.data.exception.IncorrectUsageException;
import be.atbash.runtime.core.data.parameter.ConfigurationParameters;
import be.atbash.runtime.core.module.ModuleManager;
import be.atbash.runtime.core.modules.ModulesLogger;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ModuleManagerTest {

    public static final String FAIL_CONFIG_MODULE = "be.atbash.runtime.test.config.fail";
    public static final String FAIL_LOGGING_MODULE = "be.atbash.runtime.test.logging.fail";
    public static final String FAIL_MODULE1 = "be.atbash.runtime.test.module1.fail";

    @AfterEach
    public void cleanUp() {
        ModulesLogger.clearEvents();
        System.clearProperty(FAIL_CONFIG_MODULE);
        System.clearProperty(FAIL_LOGGING_MODULE);
        System.clearProperty(FAIL_MODULE1);
    }

    @Test()
    @Order(1)
    public void getInstanceWithoutConfig() {
        Assertions.assertThatThrownBy(ModuleManager::getInstance)
                .isInstanceOf(IncorrectUsageException.class);
    }

    @Test
    @Order(3)
    public void startAndStopModules() {
        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .build();

        ConfigurationParameters parameters = new ConfigurationParameters();
        parameters.setModules("module1,module2");
        ModuleManager manager = ModuleManager.initModuleManager(parameters);
        manager.startModules();
        manager.stopModules();

        List<String> events = ModulesLogger.getEvents();

        Assertions.assertThat(events).hasSize(12);
        Assertions.assertThat(events.get(0)).isEqualTo("Start Config Module");
        Assertions.assertThat(events.get(1)).isEqualTo("End Config Module");
        Assertions.assertThat(events.get(2)).isEqualTo("Start Logging Module");
        Assertions.assertThat(events.get(3)).isEqualTo("End Logging Module");
        Assertions.assertThat(events.get(4)).isEqualTo("Start Module 1");
        Assertions.assertThat(events.get(5)).isEqualTo("End Module 1");
        Assertions.assertThat(events.get(6)).isEqualTo("Start Module 2");
        Assertions.assertThat(events.get(7)).isEqualTo("End Module 2");

        Assertions.assertThat(events.get(8)).isEqualTo("Stop Module2");
        Assertions.assertThat(events.get(9)).isEqualTo("Stop Module1");
        Assertions.assertThat(events.get(10)).isEqualTo("Stop LoggingModule");
        Assertions.assertThat(events.get(11)).isEqualTo("Stop ConfigModule");
    }

    @Test
    @Order(4)
    public void configModuleFail() {
        System.setProperty(FAIL_CONFIG_MODULE, "true");
        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .build();

        ConfigurationParameters parameters = new ConfigurationParameters();
        parameters.setModules("module1,module2");
        Assertions.assertThatThrownBy(() -> ModuleManager.initModuleManager(parameters))
                .isInstanceOf(AtbashStartupAbortException.class);

        List<String> events = ModulesLogger.getEvents();

        Assertions.assertThat(events).hasSize(1);
        Assertions.assertThat(events.get(0)).isEqualTo("Start Config Module");

    }

    @Test
    @Order(5)
    public void loggingModuleFail() {
        System.setProperty(FAIL_LOGGING_MODULE, "true");
        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .build();

        ConfigurationParameters parameters = new ConfigurationParameters();
        parameters.setModules("module1,module2");
        Assertions.assertThatThrownBy(() -> ModuleManager.initModuleManager(parameters))
                .isInstanceOf(AtbashStartupAbortException.class);

        List<String> events = ModulesLogger.getEvents();

        Assertions.assertThat(events).hasSize(3);
        Assertions.assertThat(events.get(0)).isEqualTo("Start Config Module");
        Assertions.assertThat(events.get(1)).isEqualTo("End Config Module");
        Assertions.assertThat(events.get(2)).isEqualTo("Start Logging Module");

    }

    @Test
    @Order(6)
    public void module1Fail() {
        System.setProperty(FAIL_MODULE1, "true");
        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .build();

        ConfigurationParameters parameters = new ConfigurationParameters();
        parameters.setModules("module1,module2");
        ModuleManager moduleManager = ModuleManager.initModuleManager(parameters);
        Assertions.assertThatThrownBy(moduleManager::startModules)
                .isInstanceOf(AtbashStartupAbortException.class);

        List<String> events = ModulesLogger.getEvents();

        Assertions.assertThat(events).hasSize(5);
        Assertions.assertThat(events.get(0)).isEqualTo("Start Config Module");
        Assertions.assertThat(events.get(1)).isEqualTo("End Config Module");
        Assertions.assertThat(events.get(2)).isEqualTo("Start Logging Module");
        Assertions.assertThat(events.get(3)).isEqualTo("End Logging Module");
        Assertions.assertThat(events.get(4)).isEqualTo("Start Module 1");

    }


}