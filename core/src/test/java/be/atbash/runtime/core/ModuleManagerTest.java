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


import be.atbash.runtime.core.data.exception.IncorrectUsageException;
import be.atbash.runtime.core.data.parameter.ConfigurationParameters;
import be.atbash.runtime.core.module.ModuleManager;
import be.atbash.runtime.core.modules.ModulesLogger;
import be.atbash.runtime.monitor.core.MonitorBean;
import be.atbash.runtime.monitor.core.MonitoringService;
import be.atbash.runtime.monitor.data.ServerMon;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;

import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ModuleManagerTest {

    @AfterEach
    public void cleanUp() {
        ModulesLogger.clearEvents();
    }

    @Test()
    @Order(1)
    public void getInstanceWithoutConfig() {
        Assertions.assertThatThrownBy(() -> {
            ModuleManager.getInstance();
        }).isInstanceOf(IncorrectUsageException.class);
    }

    @Test
    @Order(3)
    public void startAndStopModules() {


        MonitoringService.registerBean(MonitorBean.RuntimeMonitorBean, new ServerMon(System.currentTimeMillis()));

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


}