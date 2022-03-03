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
package be.atbash.runtime.cli;

import be.atbash.runtime.cli.command.RuntimeCommand;
import be.atbash.runtime.common.command.AbstractAtbashCommand;
import be.atbash.runtime.core.data.exception.AtbashRuntimeException;
import be.atbash.runtime.core.data.exception.UnexpectedException;
import be.atbash.runtime.core.data.exception.message.ExceptionMessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.List;

public class MainCLI {


    public static void main(String[] args) {

        Logger LOGGER = LoggerFactory.getLogger(MainCLI.class);
        ExceptionMessageUtil.addModule("runtime-cli");

        RuntimeCommand command = new RuntimeCommand();
        CommandLine commandLine = new CommandLine(command);

        AbstractAtbashCommand actualCommand = handleCommandLine(args, command, commandLine);
        if (actualCommand == null) {
            return;
            // FIXME
        }

        try {
            actualCommand.call();
        } catch (AtbashRuntimeException e) {
            // actually, this was just a way to jump to here :)
            return;
        } catch (Exception e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }

    }

    private static AbstractAtbashCommand handleCommandLine(String[] args, RuntimeCommand command, CommandLine commandLine) {
        AbstractAtbashCommand result = null;
        try {
            CommandLine.ParseResult parseResult = commandLine.parseArgs(args);

            List<CommandLine> commandLines = parseResult.asCommandLineList();
            result = commandLines.get(commandLines.size() - 1).getCommand();


        } catch (CommandLine.ParameterException e) {
            // FIXME Test if this is properly printed out
            System.out.println(e.getMessage());
            commandLine.printVersionHelp(System.out, CommandLine.Help.Ansi.AUTO);
        }
        return result;
    }

}
