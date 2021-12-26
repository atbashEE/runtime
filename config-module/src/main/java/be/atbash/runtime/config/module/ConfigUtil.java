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
package be.atbash.runtime.config.module;

import be.atbash.json.JSONValue;
import be.atbash.runtime.config.util.FileUtil;
import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.deployment.info.PersistedDeployments;

final class ConfigUtil {

    private ConfigUtil() {
    }

    public static PersistedDeployments readApplicationDeploymentsData(RuntimeConfiguration runtimeConfiguration) {

        String content = FileUtil.readDeployedApplicationsContent(runtimeConfiguration);
        return JSONValue.parse(content, PersistedDeployments.class);

    }

}
