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
package be.atbash.runtime.core.modules;

import static be.atbash.runtime.core.ModuleManagerTest.FAIL_MODULE1;
import static org.junit.jupiter.api.Assertions.fail;

public class Module1 extends AbstractTestModule {
    @Override
    public String name() {
        return "module1";
    }

    @Override
    public String[] dependencies() {
        return new String[0];
    }

    @Override
    public void run() {
        ModulesLogger.addEvent("Start Module 1");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail(e);
        }
        if (System.getProperty(FAIL_MODULE1) != null) {
            throw new IllegalStateException("Forced test exception");
        }

        ModulesLogger.addEvent("End Module 1");
    }
}
