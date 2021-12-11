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
package be.atbash.runtime.remotecli.command;

import be.atbash.runtime.common.command.data.CommandResponse;
import be.atbash.runtime.core.deployment.monitor.ApplicationInfo;
import be.atbash.runtime.core.deployment.monitor.ApplicationMon;
import be.atbash.runtime.monitor.core.MonitorBean;
import be.atbash.runtime.monitor.core.MonitoringService;
import be.atbash.runtime.monitor.data.ServerMonMBean;
import be.atbash.runtime.remotecli.util.TimeUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

import static be.atbash.runtime.common.command.RuntimeCommonConstant.CLASS_INFO_MARKER;

public class ListApplicationsRemoteCommand implements ServerRemoteCommand {
    @Override
    public CommandResponse handleCommand(Map<String, String> options) {
        CommandResponse result = new CommandResponse();
        result.setSuccess(true);

        ApplicationMon applicationMonitorBean =  MonitoringService.retrieveBean(MonitorBean.ApplicationMonitorBean);

        ObjectMapper mapper = new ObjectMapper();
        if (applicationMonitorBean.getApplications().isEmpty()) {
            result.addData(" RC-102 ", "No applications deployed");  // FIXME not really an error/info message but it is helpful I guess
        } else {
            applicationMonitorBean.getApplications()
                    .forEach(info -> {
                        try {
                            result.addData(info.getName(), mapper.writeValueAsString(info));
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    });
            result.addData(CLASS_INFO_MARKER, ApplicationInfo.class.getName());
        }
        return result;
    }
}
