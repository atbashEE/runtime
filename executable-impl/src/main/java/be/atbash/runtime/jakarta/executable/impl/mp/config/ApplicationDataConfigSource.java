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
package be.atbash.runtime.jakarta.executable.impl.mp.config;

import be.atbash.runtime.core.data.deployment.CurrentDeployment;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ApplicationDataConfigSource implements ConfigSource {

    private static final List<String> MODULE_NAMES = List.of("mp-config.", "jersey.");

    private final Set<String> propertyNames;

    private final Map<String, String> configValues;

    public ApplicationDataConfigSource() {
        Map<String, String> deploymentData = CurrentDeployment.getInstance().getCurrent()
                .getDeploymentData();
        propertyNames = deploymentData
                .keySet()
                .stream().filter(this::isNotForModule)
                .collect(Collectors.toSet());
        configValues = deploymentData.entrySet().stream()
                .filter(e -> propertyNames.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private boolean isNotForModule(String key) {
        return MODULE_NAMES.stream().noneMatch(key::startsWith);
    }

    @Override
    public Set<String> getPropertyNames() {
        return propertyNames;
    }

    @Override
    public String getValue(String propertyName) {
        return configValues.get(propertyName);
    }

    @Override
    public String getName() {
        return "ConfigSource over ApplicationData";
    }
}
