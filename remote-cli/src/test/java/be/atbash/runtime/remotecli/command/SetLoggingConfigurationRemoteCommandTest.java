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
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.watcher.WatcherService;
import be.atbash.runtime.core.module.RuntimeObjectsManager;
import be.atbash.runtime.remotecli.RuntimeObjectProvidingModule;
import be.atbash.util.TestReflectionUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SetLoggingConfigurationRemoteCommandTest {

    @Mock
    private ConfigurationManager configurationManagerMock;

    // FIXME check within the tests if the Event LOGGING_UPDATE is triggered correctly!

    @Test
    void handleCommand() throws NoSuchFieldException {
        RuntimeObjectProvidingModule module = new RuntimeObjectProvidingModule(configurationManagerMock);

        when(configurationManagerMock.setLoggingConfigCommand(new String[]{"key=value"})).thenReturn(Collections.EMPTY_LIST);

        Map<Class<?>, Module<?>> mapping = TestReflectionUtils.getValueOf(RuntimeObjectsManager.getInstance(), "runtimeObjectMapping");
        mapping.clear();
        mapping.put(ConfigurationManager.class, module);

        SetLoggingConfigurationRemoteCommand command = new SetLoggingConfigurationRemoteCommand();

        Map<String, String> options = new HashMap<>();
        options.put("", "key=value");
        CommandResponse commandResponse = command.handleCommand(options);
        Assertions.assertThat(commandResponse.isSuccess()).isTrue();

        // No file specified, so should not try
        verify(configurationManagerMock, Mockito.never()).setLoggingConfigCommand(any(File.class));
    }

    @Test
    void handleCommand_withFile() throws NoSuchFieldException {
        RuntimeObjectProvidingModule module = new RuntimeObjectProvidingModule(configurationManagerMock);

        Map<Class<?>, Module<?>> mapping = TestReflectionUtils.getValueOf(RuntimeObjectsManager.getInstance(), "runtimeObjectMapping");
        mapping.clear();
        mapping.put(ConfigurationManager.class, module);

        SetLoggingConfigurationRemoteCommand command = new SetLoggingConfigurationRemoteCommand();

        command.uploadedFile("test", new ByteArrayInputStream("theFileContent".getBytes()));

        Map<String, String> options = new HashMap<>();
        options.put("", "");
        CommandResponse commandResponse = command.handleCommand(options);

        Assertions.assertThat(commandResponse.isSuccess()).isTrue();

        // There should never be an attempt to configure options (as there is non specified)
        verify(configurationManagerMock, Mockito.never()).setLoggingConfigCommand((String[]) any());
        verify(configurationManagerMock).setLoggingConfigCommand(any(File.class));
    }
}