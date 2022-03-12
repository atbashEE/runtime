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
import be.atbash.runtime.config.ConfigurationManager;
import be.atbash.runtime.config.commands.SetLoggingConfigurationCommand;
import be.atbash.runtime.core.data.exception.UnexpectedException;
import be.atbash.runtime.core.data.module.event.EventManager;
import be.atbash.runtime.core.data.util.FileUtil;
import be.atbash.runtime.core.module.RuntimeObjectsManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static be.atbash.runtime.core.data.module.event.Events.LOGGING_UPDATE;

public class SetLoggingConfigurationRemoteCommand extends SetLoggingConfigurationCommand implements ServerRemoteCommand, HandleFileUpload {

    private final List<UploadedFile> uploadedFiles = new ArrayList<>();

    @Override
    public CommandResponse handleCommand(Map<String, String> options) {
        CommandResponse result = new CommandResponse();

        ConfigurationManager configurationManager = RuntimeObjectsManager.getInstance().getExposedObject(ConfigurationManager.class);
        if (!uploadedFiles.isEmpty()) {
            configurationManager.setLoggingConfigCommand(uploadedFiles.get(0).getTempFileLocation());
        }

        List<String> errors = new ArrayList<>();
        String[] configParameters = options.get("").split(",");
        if (!(configParameters.length == 1 && configParameters[0].isEmpty())) {
            errors.addAll(configurationManager.setLoggingConfigCommand(configParameters));
        }

        if (errors.isEmpty()) {
            EventManager.getInstance().publishEvent(LOGGING_UPDATE, new Object());
        } else {
            result.setErrorMessage(String.join(System.lineSeparator(), errors));
        }

        return result;
    }

    @Override
    public void uploadedFile(String name, InputStream inputStream) {
        try {
            uploadedFiles.add(new UploadedFile(name, FileUtil.storeStreamToTempFile(inputStream)));
        } catch (IOException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }
    }
}
