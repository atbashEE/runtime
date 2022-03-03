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
package be.atbash.runtime.config;

import be.atbash.json.JSONValue;
import be.atbash.runtime.AtbashRuntimeConstant;
import be.atbash.runtime.config.commands.AbstractConfigurationCommand;
import be.atbash.runtime.config.commands.ConfigFileCommands;
import be.atbash.runtime.config.util.ConfigFileUtil;
import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.exception.AtbashStartupAbortException;
import be.atbash.runtime.core.data.exception.UnexpectedException;
import be.atbash.runtime.core.data.module.event.EventManager;
import be.atbash.runtime.logging.LoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static be.atbash.runtime.core.data.module.event.Events.CONFIGURATION_UPDATE;

/**
 * Class responsible for executing configuration commands and executing the configuration file after modules startup.
 */
public class ConfigurationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationManager.class);
    private static final String LOG_FILE_HANDLER_PREFIX = AtbashRuntimeConstant.LOGFILEHANDLER + ".";

    private final RuntimeConfiguration runtimeConfiguration;

    public ConfigurationManager(RuntimeConfiguration runtimeConfiguration) {
        this.runtimeConfiguration = runtimeConfiguration;
    }

    public List<String> setCommand(String[] options) {
        List<String> result = new ArrayList<>();
        for (String option : options) {
            String[] parts = option.split("=");
            if (parts.length != 2) {
                String msg = LoggingUtil.formatMessage(LOGGER, "CONFIG-101", option);
                result.add(msg);
            } else {
                Optional<String> moduleName = getModuleName(parts[0]);
                if (moduleName.isEmpty()) {
                    String msg = LoggingUtil.formatMessage(LOGGER, "CONFIG-102", parts[0]);
                    result.add(msg);
                } else {
                    String key = parts[0].substring(moduleName.get().length() + 1);
                    runtimeConfiguration.getConfig().getModules().writeConfigValue(moduleName.get(), key, parts[1]);
                }
            }
        }

        if (result.isEmpty()) {
            writeConfigFile();
        }

        return result;
    }

    private void writeConfigFile() {
        String content = JSONValue.toJSONString(runtimeConfiguration.getConfig());
        ConfigFileUtil.writeConfigurationContent(runtimeConfiguration.getConfigDirectory(), runtimeConfiguration.isStateless(), content);
    }

    private Optional<String> getModuleName(String dottedName) {
        int pos = dottedName.indexOf('.');
        if (pos == -1) {
            return Optional.empty();
        } else {
            return Optional.of(dottedName.substring(0, pos));
        }
    }

    public void setLoggingConfigCommand(File propertiesFile) {
        Properties uploadedProperties = new Properties();
        try (InputStream in = new FileInputStream(propertiesFile)) {
            uploadedProperties.load(in);

        } catch (IOException e) {
            // FIXME what happens when this Exception is thrown. Does runtime stop or where is it captured?
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);

        }
        writeLoggingProperties(uploadedProperties);
    }

    public List<String> setLoggingConfigCommand(String[] options) {
        List<String> result = new ArrayList<>();
        Properties properties = readLoggingProperties();
        for (String option : options) {
            String[] parts = option.split("=");
            if (parts.length != 2) {
                String msg = LoggingUtil.formatMessage(LOGGER, "CONFIG-101", option);
                result.add(msg);
            } else {
                String key = parts[0];
                // We can't use be.atbash.runtime.logging.util.LogUtil.getLogPropertyKey Logging depends on Config
                if (!key.contains(".")) {
                    key = LOG_FILE_HANDLER_PREFIX + key;
                }
                properties.setProperty(key, parts[1]);
            }
        }

        if (result.isEmpty()) {
            writeLoggingProperties(properties);
        }
        return result;
    }

    private void writeLoggingProperties(Properties properties) {
        try (OutputStream out = new FileOutputStream(System.getProperty(AtbashRuntimeConstant.LOGGING_FILE_SYSTEM_PROPERTY))) {
            properties.store(out, ""); // TODO Sorted output?
        } catch (IOException e) {
            // FIXME what happens when this Exception is thrown. Does runtime stop or where is it captured?
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }
    }

    private Properties readLoggingProperties() {
        Properties result = new Properties();
        try (InputStream in = new FileInputStream(System.getProperty(AtbashRuntimeConstant.LOGGING_FILE_SYSTEM_PROPERTY))) {
            result.load(in);

        } catch (IOException e) {
            // FIXME what happens when this Exception is thrown. Does runtime stop or where is it captured?
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);

        }

        return result;

    }

    public void executeConfigFile(File configFile) throws Exception {
        String[] configLines;
        try {
            configLines = Files.readString(configFile.toPath()).split(System.lineSeparator());
        } catch (IOException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }

        LOGGER.atInfo().addArgument(configFile).log("CONFIG-105");
        ConfigFileCommands configFileCommands = new ConfigFileCommands();
        CommandLine commandLine = new CommandLine(configFileCommands);

        int line = 1;
        for (String configLine : configLines) {
            String command = configLine.trim();
            // Is it a comment line?
            if (command.startsWith("#")) {
                continue;
            }

            CommandLine.ParseResult parseResult;

            try {
                parseResult = commandLine.parseArgs(configLine.split(" "));
            } catch (CommandLine.ParameterException e) {
                LOGGER.atError().addArgument(line).addArgument(e.getMessage()).log("CONFIG-103");
                throw new AtbashStartupAbortException();
            }

            List<CommandLine> commandLines = parseResult.asCommandLineList();
            CommandLine actualCommandLine = commandLines.get(commandLines.size() - 1);
            AbstractConfigurationCommand currentCommand = actualCommandLine.getCommand();

            LOGGER.atInfo().addArgument(actualCommandLine.getCommandName()).addArgument(line).log("CONFIG-106");

            Integer callResult = currentCommand.call();
            // result < 0 -> Error -> abort startup.
            if (callResult < 0) {
                LOGGER.atError().addArgument(line).log("CONFIG-104");
                throw new AtbashStartupAbortException();
            }

            line++;
        }

        LOGGER.atInfo().log("CONFIG-107");
        EventManager.getInstance().publishEvent(CONFIGURATION_UPDATE, runtimeConfiguration);
    }
}
