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
package be.atbash.runtime.jetty;

import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventPayload;
import be.atbash.runtime.core.data.module.event.Events;
import be.atbash.runtime.core.data.parameter.ConfigurationParameters;
import be.atbash.runtime.core.data.parameter.WatcherType;
import be.atbash.runtime.core.data.watcher.WatcherService;
import be.atbash.runtime.core.deployment.Deployer;
import be.atbash.runtime.core.module.ModuleManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.Collections;


@ExtendWith(MockitoExtension.class)
class JettyModuleTest {

    @Mock
    private ArchiveDeployment deploymentMock;

    @Mock
    private Module moduleMock;

    @Test
    void onEvent() {
        Module module = new JettyModule();
        Mockito.when(deploymentMock.getDeploymentModule()).thenReturn(module);
        EventPayload payload = new EventPayload(Events.POST_DEPLOYMENT, deploymentMock);
        module.onEvent(payload);

        Mockito.verify(deploymentMock).setApplicationReady();
    }

    @Test
    void onEvent_otherModule() {
        Module module = new JettyModule();
        Mockito.when(deploymentMock.getDeploymentModule()).thenReturn(moduleMock);
        EventPayload payload = new EventPayload(Events.POST_DEPLOYMENT, deploymentMock);
        module.onEvent(payload);

        Mockito.verify(deploymentMock, Mockito.never()).setApplicationReady();
    }

    @Test
    void onEvent_wrongEvent() {
        Module module = new JettyModule();
        EventPayload payload = new EventPayload(Events.PRE_DEPLOYMENT, deploymentMock);
        module.onEvent(payload);

        Mockito.verify(deploymentMock, Mockito.never()).setApplicationReady();
    }
}