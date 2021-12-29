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
package be.atbash.runtime.testing.config;

import be.atbash.runtime.testing.model.JDKRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Config {

    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

    private static Double factor;

    private Config() {
    }

    public static String getVersion() {
        return System.getProperty("atbash.runtime.version", Defaults.VERSION);

    }

    public static JDKRuntime getJDKRuntime() {
        return JDKRuntime.parse(System.getProperty("atbash.test.container.jdk", Defaults.JDK_RUNTIME));
    }

    /**
     * @return The amount of time (in seconds) to wait for a runtime to start before
     * assuming that application start has failed and aborting the start process.
     * <p>
     * With the env property 'atbash.test.timeout.factor' you can increase this.
     */
    public static int getAppStartTimeout() {

        return (int) (15 * getFactor());
    }

    public static double getFactor() {
        if (factor == null) {
            factor = readFactor();
        }
        return factor;
    }

    private static Double readFactor() {
        String value = System.getenv("atbash_test_timeout_factor");
        double result = 1.0;
        if (value == null || value.isEmpty()) {
            return result;
        }
        try {
            result = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            LOGGER.warn(String.format("The env variable atbash_test_timeout_factor is not a valid number(double) : '%s'. Using factor 1.0", value));
        }
        if (result <= 0.0) {
            result = 1.0;
            LOGGER.warn(String.format("The env variable atbash_test_timeout_factor is zero or negative : '%s'. Using factor 1.0", value));
        }
        return result;
    }

    private static class Defaults {
        // read version from Maven.
        static final String VERSION = Defaults.class.getPackage().getImplementationVersion();
        static final String JDK_RUNTIME = JDKRuntime.JDK11.name();
    }
}
