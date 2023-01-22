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
import picocli.CommandLine;

import java.util.List;
import java.util.Optional;

@CommandLine.Command(name = "stop-process")
public class StopProcessCommand extends AbstractAtbashCommand {

    @CommandLine.Parameters(index = "0", arity = "0..1")
    private Long pid;

    @Override
    public Integer call() throws Exception {
        List<AtbashProcess> processes = ListProcessesCommand.getJavaProcesses();
        if (processes.isEmpty()) {
            System.out.println("CLI-213: No Atbash Runtime Processes detected, nothing to do");
            return -1;
        }

        if (pid == null && processes.size() > 1) {
            System.out.println("CLI-212: There are multiple Atbash Runtime Processes detected so a pid is required to be defined on the command-line.");
            return -1;
        }
        if (pid == null) {
            pid = processes.get(0).getPid();
        }
        Optional<AtbashProcess> process = processes.stream().filter(p -> pid.equals(p.getPid())).findAny();
        if (process.isPresent()) {
            boolean stopped = process.get().stop();
            System.out.printf("CLI-215: The stop command is issued. Was it successful? %s", stopped);
        } else {
            System.out.printf("CLI-214: There is no Atbash Runtime process found with pid '%s' ", pid);
        }

        return 0;
    }
}