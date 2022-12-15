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
package be.atbash.runtime.config.mp.sources;

import be.atbash.config.mp.sources.PropertiesConfigSource;
import be.atbash.runtime.core.data.deployment.AbstractDeployment;
import be.atbash.runtime.core.data.deployment.CurrentDeployment;
import be.atbash.runtime.core.data.exception.UnexpectedException;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ArchiveDeploymentConfigSourceProvider implements ConfigSourceProvider {

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {

        List<ConfigSource> result = new ArrayList<>();

        AbstractDeployment deployment = CurrentDeployment.getInstance().getCurrent();
        // deployment != null to be sure (and because tests don't have a current deployment
        if (deployment != null && deployment.getConfigDataFile() != null) {
            try {
                // Default ordinal 200, read from config_ordinal within file.
                result.add(new PropertiesConfigSource(deployment.getConfigDataFile().toURI().toURL(), 200));
            } catch (IOException e) {
                throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
            }
        }

        return result;
    }
}
