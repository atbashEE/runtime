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
package be.atbash.runtime.cli.command;

import picocli.CommandLine;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@CommandLine.Command(name = "set-logging-configuration")
public class CLISetLoggingConfigurationCommand extends AbstractRemoteAtbashCommand {

    @CommandLine.Option(names = {"--file"}, description = "Points to the properties files for the logging configuration.")
    private File propertiesFile;

    @CommandLine.Parameters(index = "0..*")
    private String[] options;

    @Override
    public Integer call() throws Exception {
        if (options == null && propertiesFile == null) {
            LOGGER.warn("RCLI-011: No options specified to change the logging configuration; use either --file or key=value option to define some values.");
            return -1;
        }
        Map<String, String> commandOptions = new HashMap<>();
        String data = options == null ? "" : String.join(",", options);
        commandOptions.put("", data);
        if (propertiesFile == null) {
            callRemoteCLI("POST", "set-logging-configuration", basicRemoteCLIParameters, commandOptions);
            // TODO or should it be PUT instead of POST?
        } else {
            callRemoteCLI("set-logging-configuration", basicRemoteCLIParameters, commandOptions, false, new File[]{propertiesFile});
        }
        return 0;
    }
}
