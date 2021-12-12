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
package be.atbash.runtime.common.command;

import picocli.CommandLine;

import java.util.HashMap;
import java.util.Map;

@CommandLine.Command(name = "undeploy")
public class UndeployCommand extends AbstractRemoteAtbashCommand {

    @CommandLine.Parameters(index = "0")
    private String deploymentName;

    @Override
    public Integer call() throws Exception {
        Map<String, String> options = new HashMap<>();
        options.put("name", deploymentName);

        callRemoteCLI("POST", "undeploy", basicRemoteCLIParameters, options);
        return 0;
    }
}
