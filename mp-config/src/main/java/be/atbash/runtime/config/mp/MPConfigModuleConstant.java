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
package be.atbash.runtime.config.mp;

public final class MPConfigModuleConstant {

    public static final String CONFIG_FILES = "mp-config.properties.files";  // Used by Sniffer to indicate of microprofile-config.properties is found
    public static final String ENABLED_FORCED = "enabled.forced";  // Module setting to force MPConfig enabled (enabled even when deployment doesn't has microprofile-config.properties)

    public static final String MPCONFIG_ENABLED = "mp-config.enabled";  // Is MPConfig Module enabled. if not the CDI extension doesn't add beans.

    public static final String MPCONFIG_VALIDATION_DISABLED = "mp-config.validation.enabled";  // Is MPConfig Cdi validation enabled?

    private MPConfigModuleConstant() {
    }
}
