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
package be.atbash.runtime.logging;


import be.atbash.runtime.core.PropertyPlaceholderHelper;

import java.io.*;
import java.util.Properties;
import java.util.logging.LogManager;

// Based on Payara code
public class RuntimeLogManager extends LogManager {

    @Override
    public void readConfiguration(InputStream ins) throws IOException, SecurityException {

        Properties configuration = new Properties();
        configuration.load(ins);

        // transform
        configuration = new PropertyPlaceholderHelper(System.getenv(), PropertyPlaceholderHelper.ENV_REGEX).replacePropertiesPlaceholder(configuration);

        LoggingUtil.handleConsoleHandlerLogic(configuration);
        LoggingUtil.handleLogToFileHandlerLogic(configuration);
        LoggingUtil.handleVerboseLogic(configuration);

        StringWriter writer = new StringWriter();
        configuration.store(new PrintWriter(writer), null);

        super.readConfiguration(new ByteArrayInputStream(writer.getBuffer().toString().getBytes()));
    }
}