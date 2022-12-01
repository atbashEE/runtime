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
package be.atbash.runtime.core.data.module.event;

public final class Events {

    public static final String DEPLOYMENT = "Deployment";  // For the core to start deployment
    public static final String VERIFY_DEPLOYMENT = "VerifyDeployment";  // For the core to verify if PersistedDeployment is still valid.
    public static final String UNDEPLOYMENT = "Undeployment";  // For the core to remove the deployment

    public static final String EXECUTION = "EXECUTION";  // For the core to start Jakarta Runner

    public static final String PRE_DEPLOYMENT = "PreDeployment";  // All modules get informed of the start of a deployment
    public static final String POST_DEPLOYMENT = "PostDeployment";  // All modules get informed of the end of the deployment

    public static final String CONFIGURATION_UPDATE = "ConfigurationUpdate";  // Update of the RuntimeConfiguration
    public static final String LOGGING_UPDATE = "LoggingUpdate";  // Update of the logging.properties file.

    private Events() {
    }
}
