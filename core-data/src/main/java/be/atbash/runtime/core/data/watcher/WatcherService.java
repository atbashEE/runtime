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
package be.atbash.runtime.core.data.watcher;

import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.exception.UnexpectedException;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.parameter.WatcherType;
import be.atbash.runtime.logging.LoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

public class WatcherService {
    // Class name and package used in JULLoggerAdapter so change also over there when refactored.

    private boolean jmxActive;
    private boolean flightRecorderActive;
    private boolean minimal;

    private final Map<ObjectName, Object> monitoringBeans = new HashMap<>();

    public WatcherService(WatcherType watcherType) {
        minimal = watcherType == WatcherType.MINIMAL;
        if (watcherType == WatcherType.JFR || watcherType == WatcherType.ALL) {
            minimal = false;
            flightRecorderActive = true;
        }
        if (watcherType == WatcherType.JMX || watcherType == WatcherType.ALL) {
            minimal = false;
            jmxActive = true;
        }
        if (flightRecorderActive) {
            FlightRecorderUtil.getInstance().startRecording();
        }
    }

    public void reconfigure(RuntimeConfiguration configuration) {
        jmxActive = configuration.getConfig().getMonitoring().isJmx();
        flightRecorderActive = configuration.getConfig().getMonitoring().isFlightRecorder();
        if (minimal) {
            minimal = !jmxActive && !flightRecorderActive;
        }
    }

    public void logWatcherEvent(String module, String message, boolean asInfo) {
        if (flightRecorderActive || (minimal && Module.CORE_MODULE_NAME.equals(module))) {
            FlightRecorderUtil.getInstance().emitEvent(module, message);

        }

        String callingClassName = Thread.currentThread().getStackTrace()[2].getClassName();
        Logger logger = LoggerFactory.getLogger(callingClassName);
        if (asInfo) {
            logger.info(message);
        } else {
            if (LoggingUtil.isVerbose()) {
                logger.trace(message);
            }
        }

    }

    public void registerBean(WatcherBean watcherBean, Object mbean) {
        //  TODO should/can this be a singleton?
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        ObjectName objectName;
        try {
            objectName = constructName(watcherBean);
            if (minimal || jmxActive) {
                server.registerMBean(mbean, objectName);
            }
            monitoringBeans.put(objectName, mbean);
        } catch (MalformedObjectNameException | NotCompliantMBeanException | InstanceAlreadyExistsException |
                 MBeanRegistrationException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }

    }

    private ObjectName constructName(WatcherBean watcherBean) throws MalformedObjectNameException {
        return new ObjectName(watcherBean.getHierarchyName() + ",name=" + watcherBean.getName());
    }

    public <T> T retrieveBean(WatcherBean watcherBean) {
        ObjectName objectName;
        try {
            objectName = constructName(watcherBean);
            return (T) monitoringBeans.get(objectName);
        } catch (MalformedObjectNameException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }
    }

    /**
     * Not really important in production but in testing the same JVM is used for multiple tests
     * and the same bean is registered multiple times.
     */
    public void cleanup() {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        monitoringBeans.keySet().forEach(on -> {
            try {
                server.unregisterMBean(on);
            } catch (InstanceNotFoundException e) {
                // Just ignore. TODO We could try to figure out why it is not there (only happens in tests)

            } catch (MBeanRegistrationException e) {
                throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
            }
        });
        monitoringBeans.clear();

    }
}
