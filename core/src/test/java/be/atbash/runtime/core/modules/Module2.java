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

import static org.junit.jupiter.api.Assertions.fail;

public class Module2 extends AbstractTestModule {
    @Override
    public String name() {
        return "module2";
    }

    @Override
    public String[] dependencies() {
        return new String[]{"module1"};
    }

    @Override
    public void run() {
        ModulesLogger.addEvent("Start Module 2");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail(e);
        }
        ModulesLogger.addEvent("End Module 2");
    }
}
