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
package be.atbash.runtime.config;

import be.atbash.runtime.core.data.exception.UnexpectedException;
import be.atbash.runtime.core.data.util.ResourceReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public final class ConfigInstanceUtil {

    private ConfigInstanceUtil() {
    }

    public static void processConfigInstance(ConfigInstance configInstance) {

        String rootDirectory = configInstance.getRootDirectory();
        File root = new File(rootDirectory);
        try {
            root = root.getCanonicalFile();
        } catch (IOException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001
                    , e);
        }

        if (!root.exists()) {
            if (!configInstance.isReadOnlyFlag()) {
                System.out.printf("CI-001: The specified root directory '%s' doesn't point to an existing directory%n", root.getAbsolutePath());
            }
            return;
        }

        if (!root.isDirectory()) {
            System.out.printf("CI-002: The specified root directory '%s' is not a directory%n", root.getAbsolutePath());
            return;
        }

        File configDirectory = new File(root, configInstance.getConfigName());

        configInstance.setExistingConfigDirectory(configDirectory.exists());

        if (!configDirectory.exists()) {
            boolean created = configDirectory.mkdirs();
            if (!created && !configInstance.isReadOnlyFlag()) {
                System.out.printf("CI-003: Unable to create the directory '%s'%n", configDirectory.getAbsolutePath());
                return;

            }
        } else {
            if (configInstance.isCreateCommand()) {
                System.out.printf("CI-004: The config name '%s' already exists.%n", configInstance.getConfigName());
            }
        }

        configInstance.setConfigDirectory(configDirectory);

    }

    public static void storeRuntimeConfig(ConfigInstance configInstance) {
        writeFile(configInstance, "/default.json", "config.json");
    }

    public static void storeLoggingConfig(ConfigInstance configInstance) {
       writeFile(configInstance, "/logging.properties", "logging.properties");
    }


    private static void writeFile(ConfigInstance configInstance, String source, String targetFile) {
        if (!configInstance.isValid()) {
            //
            return;
        }
        File target = new File(configInstance.getConfigDirectory(),  targetFile);

        if (target.exists()) {
            // Nothing to do since it already exists. Don't overwrite as we can overwrite users updated config
            return;
        }
        String content;
        try {
            content = ResourceReader.readResource(source);

        } catch (IOException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001
                    , e);

        }


        byte[] strToBytes = content.getBytes();

        try {
            Files.write(target.toPath(), strToBytes);
        } catch (IOException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001
                    , e);

        }
    }
}
