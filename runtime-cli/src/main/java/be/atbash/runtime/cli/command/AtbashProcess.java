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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class AtbashProcess {
    private final List<String> deployments;
    private final ProcessHandle handle;

    public AtbashProcess(ProcessHandle handle) {
        this.handle = handle;
        deployments = determineDeployments(handle.info().arguments());
    }

    private List<String> determineDeployments(Optional<String[]> arguments) {
        List<String> result = new ArrayList<>();
        if (arguments.isPresent()) {
            String[] options = arguments.get();
            boolean stopSearch = false;
            for (int i = options.length - 1; i >= 0; i--) {
                if (!stopSearch) {
                    if (options[i].contains("atbash-runtime.jar")) {
                        stopSearch = true;
                    } else {
                        if (!options[i].contains("=")) {
                            result.add(options[i]);
                        }
                    }

                }
            }
        }
        return result;
    }

    public long getPid() {
        return handle.pid();
    }

    public List<String> getDeployments() {
        return deployments;
    }

    public boolean stop() {
        return handle.destroy();
    }
}
