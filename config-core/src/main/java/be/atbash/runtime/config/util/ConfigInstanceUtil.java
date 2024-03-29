/*
 * Copyright 2021-2023 Rudy De Busscher (https://www.atbash.be)
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
import be.atbash.runtime.core.data.exception.UnexpectedException;
import be.atbash.runtime.core.data.util.FileUtil;
import be.atbash.runtime.core.data.util.ResourceReader;
import be.atbash.runtime.logging.LoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static be.atbash.runtime.config.RuntimeConfigConstants.CONFIG_FILE;
import static be.atbash.runtime.config.RuntimeConfigConstants.DEFAULT_CONFIG_FILE;
import static be.atbash.util.resource.ResourceUtil.CLASSPATH_PREFIX;

public final class ConfigInstanceUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigInstanceUtil.class);

    private ConfigInstanceUtil() {
    }

    public static void processConfigInstance(ConfigInstance configInstance) {

        String rootDirectory = configInstance.getRootDirectory();
        File root = new File(rootDirectory);

        // Does root directory exists?
        if (!root.exists()) {
            if (!configInstance.isStateless()) {
                String msg = LoggingUtil.formatMessage(LOGGER, "CONFIG-014", root.getAbsolutePath());
                writeErrorMessage(configInstance.isCreateCommand(), msg);
            }
            configInstance.invalidConfig();
            return;
        }

        // And is root directory a directory?
        if (!root.isDirectory()) {
            if (!configInstance.isStateless()) {
                String msg = LoggingUtil.formatMessage(LOGGER, "CONFIG-015", root.getAbsolutePath());
                writeErrorMessage(configInstance.isCreateCommand(), msg);
                configInstance.invalidConfig();
            }
            return;
        }

        // Construct Configuration directory.
        File configDirectory = new File(root, configInstance.getConfigName());

        // Does that already exists?
        configInstance.setExistingConfigDirectory(configDirectory.exists());

        if (!configDirectory.exists()) {
            if (configInstance.isStateless()) {
                // In readOnly (stateless) Config is not useable if not already pointing to existing directory
                configInstance.invalidConfig();
                return;
            }
            // Create the Config Directory
            boolean created = configDirectory.mkdirs();
            if (!created) {
                String msg = LoggingUtil.formatMessage(LOGGER, "CONFIG-016", configDirectory.getAbsolutePath());
                writeErrorMessage(configInstance.isCreateCommand(), msg);
                return;

            }
        } else {
            if (configInstance.isCreateCommand()) {
                // CLI Only
                String msg = LoggingUtil.formatMessage(LOGGER, "CONFIG-017", configInstance.getConfigName());
                writeErrorMessage(configInstance.isCreateCommand(), msg);
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

    /**
     * Write the Default Configuration if file not already exists.
     *
     * @param configInstance Information about the configuration this instance.
     */
    public static void storeRuntimeConfig(ConfigInstance configInstance) {
        if (!configInstance.isStateless()) {
            writeFile(configInstance, DEFAULT_CONFIG_FILE, CONFIG_FILE, false);
        }
    }

    /**
     * Write the default Logging configuration if file not already exists.
     *
     * @param configInstance Information about the configuration this instance.
     */
    public static void storeLoggingConfig(ConfigInstance configInstance) {
        String targetFile;

        if (configInstance.isStateless()) {
            if (configInstance.getLoggingConfigurationFile() != null) {
                targetFile = configInstance.getLoggingConfigurationFile();
            } else {
                // Get the temporary directory .
                targetFile = FileUtil.getTempDirectory() + "/logging.properties";
            }
        } else {
            targetFile = "/logging.properties";

            if (configInstance.getLoggingConfigurationFile() != null) {
                // Copy config file defined by user as config file within config
                try {
                    Files.copy(Path.of(configInstance.getLoggingConfigurationFile()),
                            Path.of(configInstance.getConfigDirectory().getAbsolutePath(), targetFile),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
                }
            }
        }
        String loggingConfigFile = writeFile(configInstance, CLASSPATH_PREFIX + "logging.properties", targetFile, configInstance.isStateless());
        configInstance.setLoggingConfigurationFile(loggingConfigFile);
    }


    private static String writeFile(ConfigInstance configInstance, String source, String targetFile, boolean absolute) {
        if (!configInstance.isValid()) {
            // To be on the safe side, should always be valid if correct call sequence is used.
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

        // Read the default content
        String content;
        try {
            content = ResourceReader.readResource(source);

        } catch (IOException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001
                    , e);

        }

        // Write out the content to the file.
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
