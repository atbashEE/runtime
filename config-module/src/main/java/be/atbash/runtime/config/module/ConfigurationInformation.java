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
package be.atbash.runtime.config.module;

public class ConfigurationInformation {

    private final String jdkVersion;
    private final String detailedJDKVersion;
    private final String jdkName;
    private final String vendor;

    public ConfigurationInformation() {
        jdkVersion = "JDK "+System.getProperty("java.vm.specification.version");
        detailedJDKVersion = System.getProperty("java.vm.version");
        vendor = System.getProperty("java.vm.vendor");
        jdkName = System.getProperty("java.vm.name");

        Runtime runtime = Runtime.getRuntime();
        double mem = runtime.totalMemory() / 1024.0 / 1024.0;
    }
}
