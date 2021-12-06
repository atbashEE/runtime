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
package be.atbash.runtime.monitor.core;

import be.atbash.runtime.monitor.core.util.FlightRecorderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

public final class MonitoringService {

    private static boolean flightRecorderActive = true;  // FIXME The default but config needed

    private static boolean active;
    private static Map<ObjectName, Object> monitoringBeans = new HashMap<>();

    private MonitoringService() {
    }

    public static void logMonitorEvent(String module, String message) {
        if (flightRecorderActive) {
            FlightRecorderUtil.getInstance().emitEvent(module, message);

        }
        // FIXME the slf4j -> jdk logging overwrites the logger name based on the
        // stacktrace. So we need a customer version of slf4j -> jdk (4 CLASSES)
        String callingClassName = Thread.currentThread().getStackTrace()[2].getClassName();
        Logger logger = LoggerFactory.getLogger(callingClassName);
        logger.info(message);

    }

    public static void setActive(boolean flag) {
        active = flag;
    }

    public static boolean isActive() {
        return active;
    }

    public static void registerBean(MonitorBean monitorBean, Object mbean) {
        //  TODO should/can this be a singleton?
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        ObjectName objectName;
        try {
            objectName = constructName(monitorBean);
            server.registerMBean(mbean, objectName);  // FIXME configuration to activate JMX
            monitoringBeans.put(objectName, mbean);
        } catch (MalformedObjectNameException | NotCompliantMBeanException | InstanceAlreadyExistsException | MBeanRegistrationException e) {
            e.printStackTrace();
        }

    }

    private static ObjectName constructName(MonitorBean monitorBean) throws MalformedObjectNameException {
        return new ObjectName(monitorBean.getHierarchyName() + ":name=" + monitorBean.getName());
    }

    public static <T> T retrieveBean(MonitorBean monitorBean) {
        ObjectName objectName;
        try {
            objectName = constructName(monitorBean);
            return (T) monitoringBeans.get(objectName);
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        }
        return null; //FIXME
    }
}
