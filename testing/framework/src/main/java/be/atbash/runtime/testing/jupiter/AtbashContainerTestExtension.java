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
package be.atbash.runtime.testing.jupiter;

import be.atbash.runtime.testing.config.Config;
import be.atbash.runtime.testing.model.ServerAdapterMetaData;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AtbashContainerTestExtension implements BeforeAllCallback, AfterAllCallback {

    private TestcontainersController controller;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {

        Class<?> testClass = context.getRequiredTestClass();

        // Create a controller to manipulate all containers.
        controller = new TestcontainersController(testClass);

        AtbashContainerTest atbashContainerTest = testClass.getAnnotation(AtbashContainerTest.class);

        // Define MetaData
        ServerAdapterMetaData adapterMetaData = ServerAdapterMetaData.parse(atbashContainerTest.value());
        adapterMetaData.setStartupParameters(atbashContainerTest.startupParameters());
        adapterMetaData.setDebugMode(atbashContainerTest.debug());
        adapterMetaData.setTestApplication(atbashContainerTest.testApplication());
        adapterMetaData.setTestStartupFailure(atbashContainerTest.testStartupFailure());
        adapterMetaData.setVolumeMappings(defineVolumeMappings(atbashContainerTest.volumeMapping()));

        boolean liveLogging = atbashContainerTest.liveLogging() || Boolean.parseBoolean(System.getProperty("atbash.test.container.logging.live"));

        controller.config(adapterMetaData, liveLogging);
        controller.start();

        if (adapterMetaData.isTestStartupFailure()) {
            controller.waitUntilStopped(Config.getAppStartTimeout());
        }
    }

    private Map<String, String> defineVolumeMappings(String[] volumeMapping) {
        Map<String, String> result = new HashMap<>();
        if (volumeMapping.length == 1) {
            if (!volumeMapping[0].isBlank()) {
                Assertions.fail("volumeMapping must be pairs of directories");
            }
            return result;  // a single blank item is allowed and results in empty map.
        }
        if (volumeMapping.length % 2 == 1) {
            Assertions.fail("volumeMapping must be pairs of directories");
        } else {
            for (int i = 0; i < volumeMapping.length; i = i + 2) {
                result.put(makeAbsolute(volumeMapping[i]), volumeMapping[i + 1]);
            }
        }
        return result;
    }

    private String makeAbsolute(String path) {
        return new File(path).getAbsolutePath();
    }


    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        // Make sure all containers stop after the test.
        controller.stop();
    }

}
