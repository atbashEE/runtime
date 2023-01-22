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
package be.atbash.runtime.core.data.parameter;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

class ConfigurationParametersTest {

    @Test
    void actualCommandLine() {
        ConfigurationParameters parameters = new ConfigurationParameters();
        String commandLine = parameters.actualCommandLine();
        Assertions.assertThat(commandLine).isEqualTo("--daemon=false --profile=default --verbose=false --watcher=MINIMAL --root=. --configName=default --logToConsole=false --logToFile=true --warmup=false --stateless=false");
    }

    @Test
    void actualCommandLine_withValuesSet() {
        ConfigurationParameters parameters = new ConfigurationParameters();
        parameters.setDaemon(true);
        parameters.setProfile("full");
        parameters.setVerbose(true);
        parameters.setWatcher(WatcherType.ALL);
        parameters.setRootDirectory("myRoot");
        parameters.setConfigName("TEST");
        parameters.setLogToConsole(true);
        parameters.setLogToFile(false);
        parameters.setWarmup(true);
        parameters.setStateless(true);
        String commandLine = parameters.actualCommandLine();
        Assertions.assertThat(commandLine).isEqualTo("--daemon=true --profile=full --verbose=true --watcher=ALL --root=myRoot --configName=TEST --logToConsole=true --logToFile=false --warmup=true --stateless=true");
    }

    @Test
    void actualCommandLine_WithOptionals() {
        ConfigurationParameters parameters = new ConfigurationParameters();
        parameters.setModules("JWT");
        parameters.setDeploymentDirectory(new File("./deploy-dir"));
        parameters.setContextRoot("/app1,/app2");
        parameters.setConfigFile(new File("./myConfig"));
        parameters.setConfigDataFile(new File("./myData"));
        parameters.setArchives(new File[]{new File("/path/to/app1.war"), new File("/path/to/app2.war")});
        String commandLine = parameters.actualCommandLine();
        Assertions.assertThat(commandLine).isEqualTo("--daemon=false --profile=default, --modules=JWT --verbose=false --watcher=MINIMAL --root=. --deploymentdirectory=./deploy-dir --configName=default --logToConsole=false --logToFile=true --warmup=false --stateless=false --contextRoot=/app1,/app2 --configfile=./myConfig --datafile=./myData /path/to/app1.war /path/to/app2.war");
    }
}