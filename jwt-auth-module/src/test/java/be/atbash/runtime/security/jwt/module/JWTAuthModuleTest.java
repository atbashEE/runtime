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
package be.atbash.runtime.security.jwt.module;

import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.config.Config;
import be.atbash.runtime.core.data.config.Modules;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.module.event.EventPayload;
import be.atbash.runtime.core.data.module.event.Events;
import be.atbash.runtime.security.jwt.MPJWTModuleConstant;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

import static be.atbash.runtime.jersey.JerseyModuleConstant.EXTRA_PACKAGE_NAMES;

class JWTAuthModuleTest {

    @Test
    void run() {
        JWTAuthModule module = new JWTAuthModule();

        File configDirectory = new File("./target/testDirectory");
        configDirectory.mkdirs();

        Config config = new Config();
        config.setModules(new Modules());
        RuntimeConfiguration configuration = new RuntimeConfiguration.Builder(
                configDirectory, "JUnitTest")
                .setConfig(config)
                .build();

        module.setConfig(configuration);
        module.run();

        Map<String, Map<String, String>> map = config.getModules().getConfiguration();
        Assertions.assertThat(map.keySet()).containsExactly("mp-config");
        Assertions.assertThat(map.get("mp-config").keySet()).containsExactly("enabled.forced");
    }

    @Test
    void onEvent_realmNameDefined() {
        JWTAuthModule module = new JWTAuthModule();
        ArchiveDeployment deployment = new ArchiveDeployment(new File("./applications/test.war"));

        deployment.addDeploymentData(MPJWTModuleConstant.REALM_NAME, "JWTRealm");
        deployment.addDeploymentData(EXTRA_PACKAGE_NAMES, "some.package");
        EventPayload eventPayload = new EventPayload(Events.PRE_DEPLOYMENT, deployment);
        module.onEvent(eventPayload);

        String enabled = deployment.getDeploymentData(MPJWTModuleConstant.JWTAUTH_ENABLED);
        Assertions.assertThat(enabled).isEqualTo("true");

        String packages = deployment.getDeploymentData(EXTRA_PACKAGE_NAMES);
        Assertions.assertThat(packages).isEqualTo("some.package;be.atbash.runtime.security.jwt.jaxrs");
    }

    @Test
    void onEvent_realmNameNotDefined() {
        JWTAuthModule module = new JWTAuthModule();
        ArchiveDeployment deployment = new ArchiveDeployment(new File("./applications/test.war"));

        EventPayload eventPayload = new EventPayload(Events.PRE_DEPLOYMENT, deployment);
        module.onEvent(eventPayload);

        String enabled = deployment.getDeploymentData(MPJWTModuleConstant.JWTAUTH_ENABLED);
        Assertions.assertThat(enabled).isEqualTo("false");

        String packages = deployment.getDeploymentData(EXTRA_PACKAGE_NAMES);
        Assertions.assertThat(packages).isNull();
    }
}