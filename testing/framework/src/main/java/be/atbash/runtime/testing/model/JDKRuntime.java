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
package be.atbash.runtime.testing.model;

public enum JDKRuntime {

    JDK11("")
    , JDK17("-jdk17")
    , JDK18("-jdk18")
    , JDK19("-jdk19")
    , UNKNOWN(null);

    private final String suffix;

    JDKRuntime(String suffix) {
        this.suffix = suffix;
    }


    public static JDKRuntime parse(String data) {
        JDKRuntime result = UNKNOWN;
        if ("jdk11".equalsIgnoreCase(data)) {
            result = JDK11;
        }
        if ("jdk17".equalsIgnoreCase(data)) {
            result = JDK17;
        }
        if ("jdk18".equalsIgnoreCase(data)) {
            result = JDK18;
        }
        if ("jdk19".equalsIgnoreCase(data)) {
            result = JDK19;
        }
        return result;
    }

    public String getSuffix() {
        return suffix;
    }
}
