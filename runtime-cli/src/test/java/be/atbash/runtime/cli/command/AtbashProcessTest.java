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
package be.atbash.runtime.cli.command;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AtbashProcessTest {

    @Mock
    private ProcessHandle processHandleMock;

    @Mock
    private ProcessHandle.Info infoMock;

    @Test
    void getPid() {
        Mockito.when(processHandleMock.pid()).thenReturn(1234L);
        Mockito.when(processHandleMock.info()).thenReturn(infoMock);
        Mockito.when(infoMock.arguments()).thenReturn(Optional.of(new String[]{""}));

        AtbashProcess process = new AtbashProcess(processHandleMock);
        Assertions.assertThat(process.getPid()).isEqualTo(1234);
    }

    @Test
    void getDeployments() {
        Mockito.when(processHandleMock.info()).thenReturn(infoMock);
        String archive = "path/to/app1.war";
        Mockito.when(infoMock.arguments()).thenReturn(Optional.of(parameters(archive)));

        AtbashProcess process = new AtbashProcess(processHandleMock);
        Assertions.assertThat(process.getDeployments()).containsExactly(archive);
    }

    @Test
    void getDeployments_noDeployments() {
        Mockito.when(processHandleMock.info()).thenReturn(infoMock);
        Mockito.when(infoMock.arguments()).thenReturn(Optional.of(parameters()));

        AtbashProcess process = new AtbashProcess(processHandleMock);
        Assertions.assertThat(process.getDeployments()).isEmpty();
    }

    @Test
    void getDeployments_multiple() {
        Mockito.when(processHandleMock.info()).thenReturn(infoMock);
        String archive1 = "path/to/app1.war";
        String archive2 = "path/to/app2.war";
        Mockito.when(infoMock.arguments()).thenReturn(Optional.of(parameters(archive1, archive2)));

        AtbashProcess process = new AtbashProcess(processHandleMock);
        // names are in reverse order!!
        Assertions.assertThat(process.getDeployments()).containsExactly(archive2, archive1);
    }


    private String[] parameters(String... archives) {
        String[] result = new String[5 + archives.length];
        result[0] = "-jar";
        result[1] = "atbash-runtime/atbash-runtime.jar"; // This is the main class and required for the marker
        result[2] = "--logToConsole=false";
        result[3] = "--logToFile=true";
        result[4] = "--warmup=false";
        int idx = 5;
        for (String archive : archives) {
            result[idx++] = archive;
        }
        return result;
    }

    @Test
    void stop() {
        Mockito.when(processHandleMock.info()).thenReturn(infoMock);
        Mockito.when(infoMock.arguments()).thenReturn(Optional.of(new String[]{""}));

        AtbashProcess process = new AtbashProcess(processHandleMock);
        process.stop();

        Mockito.verify(processHandleMock).destroy();
    }
}