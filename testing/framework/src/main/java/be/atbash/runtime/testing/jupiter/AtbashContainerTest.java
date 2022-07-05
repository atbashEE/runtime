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

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate the Atbash Integration Testing based extension. Test class must be extending from {@code AbstractContainerTest}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(AtbashContainerTestExtension.class)
public @interface AtbashContainerTest {

    /**
     * Defines the Docker Image which will be used for running the integration test. The value can be unspecified when
     * you want to make use of the default value.
     * In addition to the values 'default', and 'custom', also the Runtime Version and JDK version can be specified.
     * For more information, have a look at the guide.md.
     * @return Docker Image type, Version, and JDK. See guide.md.
     */

    String value() default "default";

    /**
     * Define the startup parameters that are passed on to the Runtime within the container using the `ATBASH_ARGS` environment variable.
     * @return List of parameters to be passed to the environment variable.
     */
    String[] startupParameters() default {};

    /**
     * When live logging (default is false) is activated, the server.log is send to the test run output.
     * @return true when live logging needs to be activated.
     */
    boolean liveLogging() default false;

    /**
     * Defines if a test application is deployed in the Docker Container or not. Currently a test application is always required when using the container type 'custom'.
     * @return true (default) when test application (war or ear) is deployed into the Docker Container.
     */
    boolean testApplication() default true;

    /**
     * When set to true, the goal of the test is to test of the runtime start up fails.  Mainly for incorrect or incomplete options.
     * @return false (default) container startup on health endpoint, true the check is based on one of the log messages.
     */
    boolean testStartupFailure() default false;

    /**
     * When debug activated, the JVM option to start in debug mode (with suspended=y) is added and the timeout is increased to 120 seconds.
     * This gives the developer the time to connect a remote debugger to the Container process.
     * @return true when JVM needs to be started in debug mode.
     */
    boolean debug() default false;

    /**
     * Defines a volume mapping between the host and the container in read-write mode. The array most always contain
     * a multiple of 2 items. The first one is the directory on the host, the second in the container.
     * An exception on the multiple of 2 is when there is only 1 but is an empty String
     * @return Mapping pairs for the volume mapping.
     */
    String[] volumeMapping() default "";
}
