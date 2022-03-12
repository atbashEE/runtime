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
package be.atbash.runtime.remotecli.command;

import be.atbash.runtime.common.command.data.CommandResponse;
import be.atbash.runtime.core.data.module.event.EventManager;
import be.atbash.runtime.core.data.module.event.Events;
import be.atbash.runtime.core.data.watcher.WatcherBean;
import be.atbash.runtime.core.data.watcher.WatcherService;
import be.atbash.runtime.core.deployment.monitor.ApplicationInfo;
import be.atbash.runtime.core.deployment.monitor.ApplicationMon;
import be.atbash.runtime.core.module.RuntimeObjectsManager;

import java.util.Map;
import java.util.Optional;

public class UndeployRemoteCommand implements ServerRemoteCommand {
    @Override
    public CommandResponse handleCommand(Map<String, String> options) {
        CommandResponse result = new CommandResponse();

        String name = options.get("name");

        WatcherService watcherService = RuntimeObjectsManager.getInstance().getExposedObject(WatcherService.class);
        ApplicationMon applicationMonitorBean = watcherService.retrieveBean(WatcherBean.ApplicationWatcherBean);
        Optional<ApplicationInfo> applicationInfo = applicationMonitorBean.getApplications()
                .stream().filter(ai -> ai.getName().equals(name))
                .findAny();

        if (applicationInfo.isPresent()) {
            EventManager eventManager = EventManager.getInstance();
            eventManager.publishEvent(Events.UNDEPLOYMENT, name);
            result.addData(applicationInfo.get().getName(), "Application is removed from the Runtime.");
        } else {
            result.setErrorMessage(String.format("RC-103: Unable to find the application with name '%s'", name));
        }
        return result;
    }
}
