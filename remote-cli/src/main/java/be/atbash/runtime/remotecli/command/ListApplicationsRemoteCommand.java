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
import be.atbash.runtime.core.data.RunData;
import be.atbash.runtime.core.data.exception.UnexpectedException;
import be.atbash.runtime.core.deployment.monitor.ApplicationInfo;
import be.atbash.runtime.core.module.RuntimeObjectsManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

import static be.atbash.runtime.common.command.RuntimeCommonConstant.CLASS_INFO_MARKER;

public class ListApplicationsRemoteCommand implements ServerRemoteCommand {
    @Override
    public CommandResponse handleCommand(Map<String, String> options) {
        CommandResponse result = new CommandResponse();
        result.setSuccess(true);

        RunData runData = RuntimeObjectsManager.getInstance().getExposedObject(RunData.class);

        ObjectMapper mapper = new ObjectMapper();
        if (runData.getDeployments().isEmpty()) {
            result.addData(" RC-102 ", "No applications deployed");  // FIXME not really an error/info message but it is helpful I guess
        } else {
            runData.getDeployments()
                    .forEach(ad -> {
                        try {
                            ApplicationInfo info = new ApplicationInfo(ad);
                            result.addData(ad.getDeploymentName(), mapper.writeValueAsString(info));
                        } catch (JsonProcessingException e) {
                            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
                        }
                    });
            result.addData(CLASS_INFO_MARKER, ApplicationInfo.class.getName());
        }
        return result;
    }
}
