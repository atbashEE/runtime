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
package be.atbash.runtime.demo.rest;

import be.atbash.runtime.jakarta.executable.JakartaSERunnerBuilder;

public class JakartaApplication {

    public static void main(String[] args) {

        // This uses scanning of the package to discover the JAX-RS resources.
        JakartaSERunnerBuilder.newBuilder(DemoApplication.class)
                .withPort(8888)
                .run();

        /*
        // Define the JAX-RS resources.
        // Does not take into account @ApplicationPath (since you didn't specify it when running)
        JakartaSERunnerBuilder.newBuilder(HelloResource.class, PersonResource.class)
                .withPort(8888)
                .run();
        */

    }
}
