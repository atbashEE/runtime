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
package be.atbash.runtime.testing.arquillian;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;

/**
 * Configuration of Atbash for Arquillian.
 */
public class AtbashContainerConfiguration implements ContainerConfiguration {

    // Is the archive kept in temp directory?
    private boolean keepArchive;

    @Override
    public void validate() throws ConfigurationException {

    }

    public boolean isKeepArchive() {
        return keepArchive;
    }

    public void setKeepArchive(boolean keepArchive) {
        this.keepArchive = keepArchive;
    }
}
