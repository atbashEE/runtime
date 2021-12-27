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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static be.atbash.runtime.config.RuntimeConfigConstants.CONFIG_FILE;
import static be.atbash.runtime.config.RuntimeConfigConstants.DEFAULT_CONFIG_FILE;

public final class ConfigInstanceUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigInstanceUtil.class);

    private ConfigInstanceUtil() {
    }

    public static void processConfigInstance(ConfigInstance configInstance) {

        String rootDirectory = configInstance.getRootDirectory();
        File root = new File(rootDirectory);
        // if readonly, root Directory and Config name doesn't matter.
        if (configInstance.isReadOnlyFlag()) {
            configInstance.setConfigDirectory(root);
            // NO further processing needed
            return;
        }

        try {
            root = root.getCanonicalFile();
        } catch (IOException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001
                    , e);
        }

        if (!root.exists()) {
            writeErrorMessage(configInstance.isCreateCommand(), String.format("CONFIG-014: The specified root directory '%s' doesn't point to an existing directory", root.getAbsolutePath()));
            return;
        }

        if (!root.isDirectory()) {
            writeErrorMessage(configInstance.isCreateCommand(), String.format("CONFIG-015: The specified root directory '%s' is not a directory", root.getAbsolutePath()));
            return;
        }

        File configDirectory = new File(root, configInstance.getConfigName());

        configInstance.setExistingConfigDirectory(configDirectory.exists());

        if (!configDirectory.exists()) {
            boolean created = configDirectory.mkdirs();
            if (!created) {
                writeErrorMessage(configInstance.isCreateCommand(), String.format("CONFIG-016: Unable to create the directory '%s'", configDirectory.getAbsolutePath()));
                return;

            }
        } else {
            if (configInstance.isCreateCommand()) {
                // CLI Only
                writeErrorMessage(configInstance.isCreateCommand(), String.format("CONFIG-017: The config name '%s' already exists.", configInstance.getConfigName()));
            }
        }

        configInstance.setConfigDirectory(configDirectory);

    }

    private static void writeErrorMessage(boolean isCLI, String message) {
        if (isCLI) {
            System.out.println(message);
        } else {
            LOGGER.error(message);
        }
    }

    public static void storeRuntimeConfig(ConfigInstance configInstance) {
        if (!configInstance.isReadOnlyFlag()) {
            writeFile(configInstance, DEFAULT_CONFIG_FILE, CONFIG_FILE, false);
        }
    }

    public static void storeLoggingConfig(ConfigInstance configInstance) {
        String targetFile;

        if (configInstance.isReadOnlyFlag()) {
            String property = "java.io.tmpdir";

            // Get the temporary directory .
            targetFile = System.getProperty(property) + "logging.properties";
        } else {
            targetFile = "/logging.properties";
        }
        String loggingConfigFile = writeFile(configInstance, "/logging.properties", targetFile, configInstance.isReadOnlyFlag());
        configInstance.setLoggingConfigurationFile(loggingConfigFile);
    }


    private static String writeFile(ConfigInstance configInstance, String source, String targetFile, boolean absolute) {
        if (!configInstance.isValid()) {
            //
            return null;
        }

        File target;
        if (absolute) {
            target = new File(targetFile);
        } else {
            target = new File(configInstance.getConfigDirectory(), targetFile);
        }

        if (target.exists()) {
            // Nothing to do since it already exists. Don't overwrite as we can overwrite users updated config
            return target.getAbsolutePath();
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
        return target.getAbsolutePath();
    }
}
