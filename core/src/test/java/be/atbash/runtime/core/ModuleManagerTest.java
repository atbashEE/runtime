/*
 * Copyright 2021-2023 Rudy De Busscher (https://www.atbash.be)
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
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.parameter.ConfigurationParameters;
import be.atbash.runtime.core.data.parameter.WatcherType;
import be.atbash.runtime.core.module.ModuleManager;
import be.atbash.runtime.core.modules.ModulesLogger;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
        System.clearProperty("traceModuleStartProcessing");
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
        //System.setProperty("traceModuleStartProcessing", "true");
        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .build();

        ConfigurationParameters parameters = new ConfigurationParameters();
        // Since we are not using the real ConfigModule, modules need to be correctly set, including the 'default' modules.
        setModules(parameters, "module1", "module2");

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

    private void setModules(ConfigurationParameters parameters, String... moduleNames) {
        List<String> modules = new ArrayList<>(Arrays.asList(moduleNames));
        modules.addAll(List.of(Module.CORE_MODULE_NAME, Module.LOGGING_MODULE_NAME, Module.CONFIG_MODULE_NAME));
        parameters.setModules(String.join(",", modules));
    }

    @Test
    @Order(4)
    public void configModuleFail() {
        //System.setProperty("traceModuleStartProcessing", "true");
        System.setProperty(FAIL_CONFIG_MODULE, "true");
        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        ConfigurationParameters parameters = new ConfigurationParameters();
        // Since we are not using the real ConfigModule, modules need to be correctly set, including the 'default' modules.
        setModules(parameters, "module1", "module2");

        ModuleManager manager = ModuleManager.initModuleManager(parameters);

        Assertions.assertThatThrownBy(manager::startModules)
                .isInstanceOf(AtbashStartupAbortException.class);

        List<String> events = ModulesLogger.getEvents();

        Assertions.assertThat(events).hasSize(1);
        Assertions.assertThat(events.get(0)).isEqualTo("Start Config Module");

    }

    @Test
    @Order(5)
    public void loggingModuleFail() {
        //System.setProperty("traceModuleStartProcessing", "true");
        System.setProperty(FAIL_LOGGING_MODULE, "true");
        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        ConfigurationParameters parameters = new ConfigurationParameters();
        // Since we are not using the real ConfigModule, modules need to be correctly set, including the 'default' modules.
        setModules(parameters, "module1", "module2");


        ModuleManager manager = ModuleManager.initModuleManager(parameters);

        Assertions.assertThatThrownBy(manager::startModules)
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
        //System.setProperty("traceModuleStartProcessing", "true");
        System.setProperty(FAIL_MODULE1, "true");
        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        ConfigurationParameters parameters = new ConfigurationParameters();
        // Since we are not using the real ConfigModule, modules need to be correctly set, including the 'default' modules.
        setModules(parameters, "module1", "module2");

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

    @Test
    @Order(7)
    public void retry_afterFailure() {
        //System.setProperty("traceModuleStartProcessing", "true");
        System.setProperty(FAIL_MODULE1, "true");
        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        ConfigurationParameters parameters = new ConfigurationParameters();
        // Since we are not using the real ConfigModule, modules need to be correctly set, including the 'default' modules.
        setModules(parameters, "module1", "module2");


        ModuleManager moduleManager = ModuleManager.initModuleManager(parameters);
        Assertions.assertThatThrownBy(moduleManager::startModules)
                .isInstanceOf(AtbashStartupAbortException.class);

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

    @Test
    @Order(8)
    public void startAndStopModules_twice() {
        //System.setProperty("traceModuleStartProcessing", "true");
        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .build();

        ConfigurationParameters parameters = new ConfigurationParameters();
        parameters.setWatcher(WatcherType.OFF);
        // Since we are not using the real ConfigModule, modules need to be correctly set, including the 'default' modules.
        setModules(parameters, "module1", "module2");

        ModuleManager manager = ModuleManager.initModuleManager(parameters);
        manager.startModules();
        manager.stopModules();
        manager.startModules();
        manager.stopModules();

        List<String> events = ModulesLogger.getEvents();

        Assertions.assertThat(events).hasSize(24);
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

        Assertions.assertThat(events.get(12)).isEqualTo("Start Config Module");
        Assertions.assertThat(events.get(13)).isEqualTo("End Config Module");
        Assertions.assertThat(events.get(14)).isEqualTo("Start Logging Module");
        Assertions.assertThat(events.get(15)).isEqualTo("End Logging Module");
        Assertions.assertThat(events.get(16)).isEqualTo("Start Module 1");
        Assertions.assertThat(events.get(17)).isEqualTo("End Module 1");
        Assertions.assertThat(events.get(18)).isEqualTo("Start Module 2");
        Assertions.assertThat(events.get(19)).isEqualTo("End Module 2");

        Assertions.assertThat(events.get(20)).isEqualTo("Stop Module2");
        Assertions.assertThat(events.get(21)).isEqualTo("Stop Module1");
        Assertions.assertThat(events.get(22)).isEqualTo("Stop LoggingModule");
        Assertions.assertThat(events.get(23)).isEqualTo("Stop ConfigModule");

    }


    @Test
    @Order(9)
    public void startAndStopModules_parallelStart() {
        //System.setProperty("traceModuleStartProcessing", "true");
        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .build();

        ConfigurationParameters parameters = new ConfigurationParameters();
        parameters.setWatcher(WatcherType.OFF);
        // Since we are not using the real ConfigModule, modules need to be correctly set, including the 'default' modules.
        setModules(parameters, "module1", "module2", "module3");

        ModuleManager manager = ModuleManager.initModuleManager(parameters);
        manager.startModules();
        manager.stopModules();

        List<String> events = ModulesLogger.getEvents();

        Assertions.assertThat(events).hasSize(15);
        Assertions.assertThat(events.get(0)).isEqualTo("Start Config Module");
        Assertions.assertThat(events.get(1)).isEqualTo("End Config Module");
        Assertions.assertThat(events.get(2)).isEqualTo("Start Logging Module");
        Assertions.assertThat(events.get(3)).isEqualTo("End Logging Module");
        Assertions.assertThat(events.get(4)).isEqualTo("Start Module 1");
        Assertions.assertThat(events.get(5)).isEqualTo("Start Module 3");
        Assertions.assertThat(events.get(6)).isEqualTo("End Module 3");
        Assertions.assertThat(events.get(7)).isEqualTo("End Module 1");
        Assertions.assertThat(events.get(8)).isEqualTo("Start Module 2");
        Assertions.assertThat(events.get(9)).isEqualTo("End Module 2");

        Assertions.assertThat(events.get(10)).isEqualTo("Stop Module2");
        Assertions.assertThat(events.get(11)).isEqualTo("Stop Module1");
        Assertions.assertThat(events.get(12)).isEqualTo("Stop Module3");
        Assertions.assertThat(events.get(13)).isEqualTo("Stop LoggingModule");
        Assertions.assertThat(events.get(14)).isEqualTo("Stop ConfigModule");
    }

    @Test
    @Order(10)
    public void startAndStopModules_specialModuleName() {
        //System.setProperty("traceModuleStartProcessing", "true");
        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        RuntimeConfiguration runtimeConfiguration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .build();

        ConfigurationParameters parameters = new ConfigurationParameters();
        // Since we are not using the real ConfigModule, modules need to be correctly set, including the 'default' modules.
        setModules(parameters, "module1Special", "module4");

        ModuleManager manager = ModuleManager.initModuleManager(parameters);
        manager.startModules();
        manager.stopModules();

        List<String> events = ModulesLogger.getEvents();

        Assertions.assertThat(events).hasSize(12);

        Assertions.assertThat(events.get(0)).isEqualTo("Start Config Module");
        Assertions.assertThat(events.get(1)).isEqualTo("End Config Module");
        Assertions.assertThat(events.get(2)).isEqualTo("Start Logging Module");
        Assertions.assertThat(events.get(3)).isEqualTo("End Logging Module");
        Assertions.assertThat(events.get(4)).isEqualTo("Start Module 1 Special");
        Assertions.assertThat(events.get(5)).isEqualTo("End Module 1 Special");
        Assertions.assertThat(events.get(6)).isEqualTo("Start Module 4");
        Assertions.assertThat(events.get(7)).isEqualTo("End Module 4");

        Assertions.assertThat(events.get(8)).isEqualTo("Stop Module4");
        Assertions.assertThat(events.get(9)).isEqualTo("Stop Module1Special");
        Assertions.assertThat(events.get(10)).isEqualTo("Stop LoggingModule");
        Assertions.assertThat(events.get(11)).isEqualTo("Stop ConfigModule");
    }
}