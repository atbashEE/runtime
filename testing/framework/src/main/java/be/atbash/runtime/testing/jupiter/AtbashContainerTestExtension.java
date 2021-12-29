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
package be.atbash.runtime.testing.jupiter;

import be.atbash.runtime.testing.config.Config;
import be.atbash.runtime.testing.model.ServerAdapterMetaData;
import org.junit.jupiter.api.extension.*;

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
        adapterMetaData.setTestApplication(atbashContainerTest.testApplication());
        adapterMetaData.setTestStartupFailure(atbashContainerTest.testStartupFailure());

        boolean liveLogging = atbashContainerTest.liveLogging() || Boolean.parseBoolean(System.getProperty("atbash.test.container.logging.live"));

        controller.config(adapterMetaData, liveLogging);
        controller.start();

        if (adapterMetaData.isTestStartupFailure()) {
            controller.waitUntilStopped(Config.getAppStartTimeout());
        }
    }


    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        // Make sure all containers stop after the test.
        controller.stop();
    }

}
