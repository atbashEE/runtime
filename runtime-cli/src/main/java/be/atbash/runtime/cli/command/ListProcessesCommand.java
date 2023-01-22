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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.ProcessHandle.allProcesses;

@CommandLine.Command(name = "list-processes")
public class ListProcessesCommand extends AbstractAtbashCommand {

    @Override
    public Integer call() throws Exception {
        List<AtbashProcess> processes = getJavaProcesses();
        System.out.println("CLI-202: These are the Atbash Runtime processes");
        for (AtbashProcess process : processes) {
            System.out.printf("PID %d - archives %s%n", process.getPid(), String.join(" ", process.getDeployments()));
        }
        return 0;
    }

    public static List<AtbashProcess> getJavaProcesses() {

        return allProcesses()
                .filter(handle -> handle.info().command().isPresent() && handle.info().command().get().contains("java"))
                .filter(handle -> isAtbashRuntime(handle.info().arguments()))
                .map(AtbashProcess::new)
                .collect(Collectors.toList());
    }

    private static boolean isAtbashRuntime(Optional<String[]> arguments) {
        boolean result = false;
        if (arguments.isPresent()) {
            result = Arrays.stream(arguments.get())
                    .anyMatch(s -> s.contains("atbash-runtime.jar"));
        }
        return result;
    }


}