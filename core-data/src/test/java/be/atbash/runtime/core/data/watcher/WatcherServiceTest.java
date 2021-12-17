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
package be.atbash.runtime.core.data.watcher;

import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.config.Config;
import be.atbash.runtime.core.data.config.Monitoring;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.parameter.WatcherType;
import be.atbash.runtime.core.data.watcher.model.ServerMon;
import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WatcherServiceTest {

    @BeforeAll
    public static void setup() {
        System.setProperty("be.atbash.runtime.test", "Active");
    }

    @Test
    void registerBean() throws MalformedObjectNameException {
        ServerMon serverMon = new ServerMon(System.currentTimeMillis());
        WatcherService service = new WatcherService(WatcherType.OFF);

        service.registerBean(WatcherBean.RuntimeWatcherBean, serverMon);

        Object retrieved = service.retrieveBean(WatcherBean.RuntimeWatcherBean);
        assertThat(retrieved).isEqualTo(serverMon);

        // No JMX entry created
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("Atbash.Runtime:name=*");
        Set<ObjectInstance> objectInstances = server.queryMBeans(name, null);
        assertThat(objectInstances).isEmpty();

    }

    @Test
    void registerBean_withJMX() throws MalformedObjectNameException {
        ServerMon serverMon = new ServerMon(System.currentTimeMillis());
        WatcherService service = new WatcherService(WatcherType.JMX);

        service.registerBean(WatcherBean.RuntimeWatcherBean, serverMon);

        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("Atbash.Runtime:name=*");
        Set<ObjectInstance> objectInstances = server.queryMBeans(name, null);
        assertThat(objectInstances.size()).isEqualTo(1);
        assertThat(objectInstances.iterator().next().getClassName()).isEqualTo(ServerMon.class.getName());
    }

    @Test
    void testJFR_minimal_core() {
        WatcherService service = new WatcherService(WatcherType.MINIMAL);

        JFRTestUtil.startFlightRecorder();

        service.logWatcherEvent(Module.CORE_MODULE_NAME, "Our message");

        List<RecordedEvent> events = JFRTestUtil.stopAndReadEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType().getName()).isEqualTo("be.atbash.runtime.event");
        assertThat(events.get(0).getString("message")).isEqualTo("Our message");
    }

    @Test
    void testJFR_minimal_notcore() {
        WatcherService service = new WatcherService(WatcherType.MINIMAL);

        JFRTestUtil.startFlightRecorder();

        service.logWatcherEvent(Module.CONFIG_MODULE_NAME, "Our message");

        List<RecordedEvent> events = JFRTestUtil.stopAndReadEvents();
        assertThat(events).isEmpty();
    }

    @Test
    void testJFR_jfr() {
        WatcherService service = new WatcherService(WatcherType.JFR);

        JFRTestUtil.startFlightRecorder();

        service.logWatcherEvent(Module.CONFIG_MODULE_NAME, "Our message");

        List<RecordedEvent> events = JFRTestUtil.stopAndReadEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType().getName()).isEqualTo("be.atbash.runtime.event");
        assertThat(events.get(0).getString("message")).isEqualTo("Our message");
    }

    @Test
    void testJFR_off() {
        WatcherService service = new WatcherService(WatcherType.OFF);

        JFRTestUtil.startFlightRecorder();

        service.logWatcherEvent(Module.CONFIG_MODULE_NAME, "Our message");

        List<RecordedEvent> events = JFRTestUtil.stopAndReadEvents();
        assertThat(events).isEmpty();
    }

    @Test
    void testJFR_all() {
        WatcherService service = new WatcherService(WatcherType.ALL);

        JFRTestUtil.startFlightRecorder();

        service.logWatcherEvent(Module.CONFIG_MODULE_NAME, "Our message");

        List<RecordedEvent> events = JFRTestUtil.stopAndReadEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType().getName()).isEqualTo("be.atbash.runtime.event");
        assertThat(events.get(0).getString("message")).isEqualTo("Our message");
    }

    @Test
    void testJFR_minimal_notcore_updatetojfr() {
        WatcherService service = new WatcherService(WatcherType.MINIMAL);

        RuntimeConfiguration configuration = buildConfiguration();


        service.reconfigure(configuration);
        JFRTestUtil.startFlightRecorder();

        service.logWatcherEvent(Module.CONFIG_MODULE_NAME, "Our message");

        List<RecordedEvent> events = JFRTestUtil.stopAndReadEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType().getName()).isEqualTo("be.atbash.runtime.event");
        assertThat(events.get(0).getString("message")).isEqualTo("Our message");

    }

    private RuntimeConfiguration buildConfiguration() {
        File configDirectory = new File("./target/testDirectory1");
        configDirectory.mkdirs();

        Monitoring monitoring = new Monitoring();
        monitoring.setFlightRecorder(true);
        Config config = new Config();
        config.setMonitoring(monitoring);
        RuntimeConfiguration configuration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .setConfig(config)
                .build();
        return configuration;
    }
}