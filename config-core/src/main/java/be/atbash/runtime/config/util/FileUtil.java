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
package be.atbash.runtime.config.util;

import be.atbash.runtime.config.ConfigInstance;
import be.atbash.runtime.core.data.RuntimeConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static be.atbash.runtime.config.RuntimeConfigConstants.APPLICATIONS_FILE;

public final class FileUtil {

    private FileUtil() {
    }

    public static String readConfigurationContent(ConfigInstance configInstance) {
        File configFile = new File(configInstance.getConfigDirectory(), "config.json");
        try {
            return Files.readString(configFile.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            // FIXME
        }
        return null;
    }

    public static String readDeployedApplicationsContent(RuntimeConfiguration runtimeConfiguration) {
        File applicationFile = new File(runtimeConfiguration.getConfigDirectory(), APPLICATIONS_FILE);
        if (!applicationFile.exists()) {
            return "{}"; // empty object
        }
        try {
            return Files.readString(applicationFile.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            // FIXME
        }
        return null;
    }

    public static void writeDeployedApplicationsContent(RuntimeConfiguration runtimeConfiguration, String content) {
        File applicationFile = new File(runtimeConfiguration.getConfigDirectory(), APPLICATIONS_FILE);
        try {
            if (!applicationFile.exists()) {
                applicationFile.createNewFile();
            }

            Files.writeString(applicationFile.toPath(), content);
        } catch (IOException e) {
            e.printStackTrace();
            // FIXME
        }
    }
}
