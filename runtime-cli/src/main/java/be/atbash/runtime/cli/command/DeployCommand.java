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
package be.atbash.runtime.cli.command;

import picocli.CommandLine;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@CommandLine.Command(name = "deploy")
public class DeployCommand extends AbstractRemoteAtbashCommand {

    @CommandLine.Option(names = {"--contextroot"}, description = "The context root for the application. Comma separated list when multiple applications are deployed.")
    private String contextRoot = "";

    @CommandLine.Parameters(index = "0..*")
    private File[] archives;

    @Override
    public Integer call() throws Exception {
        Map<String, String> options = new HashMap<>();
        options.put("contextroot", contextRoot);

        callRemoteCLI("deploy", basicRemoteCLIParameters, options, archives);
        return 0;
    }

    public String getContextRoot() {
        return contextRoot;
    }

    public void setContextRoot(String contextRoot) {
        this.contextRoot = contextRoot;
    }

    public File[] getArchives() {
        return archives;
    }

    public void setArchives(File[] archives) {
        this.archives = archives;
    }
}
