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
package be.atbash.runtime.cli.command;

import be.atbash.runtime.common.command.AbstractAtbashCommand;
import be.atbash.runtime.core.data.exception.UnexpectedException;
import be.atbash.runtime.core.data.parameter.ConfigurationParameters;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CommandLine.Command(subcommands =
        {CreateConfigCommand.class,
                ListProcessesCommand.class,
                StopProcessCommand.class,
                StatusCommand.class,
                DeployCommand.class,
                ListApplicationsCommand.class,
                UndeployCommand.class,
                CLISetCommand.class,
                CLISetLoggingConfigurationCommand.class}
        , name = "")
public class RuntimeCommand extends AbstractAtbashCommand {

    @CommandLine.Mixin
    private ConfigurationParameters configurationParameters;

    @CommandLine.Option(names = {"--runtime-jar"}, description = "Location of the 'Atbash-runtime.jar' executable. By default the current directory ")
    private File runtimeJar = new File(".");

    @Override
    public Integer call() throws Exception {
        List<String> command = new ArrayList<>();

        // Find Java program
        String javaHome = System.getProperty("java.home");
        String javaCommand = new File(javaHome, "bin/java").getPath();
        command.add(javaCommand);

        // Get the runtime MX bean
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();

        // Get the input arguments passed to the JVM
        command.addAll(runtimeMxBean.getInputArguments());

        command.add("-jar");
        command.add(runtimeJar.getCanonicalPath());

        String commandOptions = getRuntimeCommandOptionsForDaemon();
        command.addAll(Arrays.asList(commandOptions.split(" ")));

        ProcessBuilder builder = new ProcessBuilder(command.toArray(new String[]{}));
        Process process;

        try {
            process = builder.start();
        } catch (IOException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }


        LoggerFactory.getLogger(RuntimeCommand.class).atInfo().addArgument(process.pid()).log("CLI-116: The runtime is started in the background with process id ''{0}''.");
        return 0;
    }

    private String getRuntimeCommandOptionsForDaemon() {
        configurationParameters.setLogToFile(true);
        configurationParameters.setLogToConsole(false);
        configurationParameters.setDaemon(false);

        return configurationParameters.actualCommandLine();
    }
}
