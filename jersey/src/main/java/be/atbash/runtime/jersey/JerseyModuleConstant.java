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
package be.atbash.runtime.jersey;

public final class JerseyModuleConstant {

    // The comma separated list of package names detected by the RestSniffer.
    public static final String PACKAGE_NAMES = "jersey.package.names";

    // The comma separated list of class names detected by the RestSniffer.
    public static final String CLASS_NAMES = "jersey.class.names";

    // The comma separated list of packages that contain providers added by some modules.
    public static final String EXTRA_PACKAGE_NAMES = "jersey.extra.package.names";
    public static final String APPLICATION_PATH = "jersey.application.path";

    private JerseyModuleConstant() {
    }
}
