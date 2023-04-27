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
package be.atbash.runtime.jersey.util;

import be.atbash.runtime.core.data.deployment.AbstractDeployment;
import be.atbash.runtime.jersey.JerseyModuleConstant;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class ExtraPackagesUtil {

    private ExtraPackagesUtil() {
    }

    public static void addPackages(AbstractDeployment deployment, String... packages) {
        StringBuilder packageNames = new StringBuilder();
        Optional<String> currentNames = Optional.ofNullable(deployment.getDeploymentData(JerseyModuleConstant.EXTRA_PACKAGE_NAMES));
        packageNames.append(currentNames.orElse(""));

        Arrays.stream(packages).forEach(name -> addName(packageNames, name));
        deployment.addDeploymentData(JerseyModuleConstant.EXTRA_PACKAGE_NAMES, packageNames.toString());

    }
    public static void addPackages(AbstractDeployment deployment, List<String> packages) {
        StringBuilder packageNames = new StringBuilder();
        Optional<String> currentNames = Optional.ofNullable(deployment.getDeploymentData(JerseyModuleConstant.EXTRA_PACKAGE_NAMES));
        packageNames.append(currentNames.orElse(""));

        packages.forEach(name -> addName(packageNames, name));
        deployment.addDeploymentData(JerseyModuleConstant.EXTRA_PACKAGE_NAMES, packageNames.toString());

    }

    private static void addName(StringBuilder packageNames, String name) {
        if (packageNames.length() > 0) {
            packageNames.append(';');
        }
        packageNames.append(name);
    }
}
