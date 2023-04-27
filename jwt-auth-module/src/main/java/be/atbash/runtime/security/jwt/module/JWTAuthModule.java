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

import be.atbash.runtime.config.mp.module.MPConfigModule;
import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.deployment.AbstractDeployment;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventPayload;
import be.atbash.runtime.core.data.module.event.Events;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.jersey.JerseyModule;
import be.atbash.runtime.jersey.util.ExtraPackagesUtil;
import be.atbash.runtime.security.jwt.MPJWTModuleConstant;

import java.util.List;

import static be.atbash.runtime.config.mp.MPConfigModuleConstant.ENABLED_FORCED;
import static be.atbash.runtime.config.mp.module.MPConfigModule.MP_CONFIG_MODULE_NAME;

public class JWTAuthModule implements Module<RuntimeConfiguration> {

    // Public so that it can be used in the Jakarta Runner to define additional modules.
    public static final String JWT_AUTH_MODULE_NAME = "mp-jwt";

    private RuntimeConfiguration configuration;

    @Override
    public String name() {
        return JWT_AUTH_MODULE_NAME;
    }

    @Override
    public String[] dependencies() {
        return new String[]{MPConfigModule.MP_CONFIG_MODULE_NAME, JerseyModule.JERSEY_MODULE_NAME};
    }

    @Override
    public Specification[] provideSpecifications() {
        return new Specification[0];
    }

    @Override
    public Class<? extends Sniffer> moduleSniffer() {
        return JWTAuthSniffer.class;
    }

    @Override
    public List<Class<?>> getRuntimeObjectTypes() {
        return null;
    }

    @Override
    public <T> T getRuntimeObject(Class<T> exposedObjectType) {
        return null;
    }

    @Override
    public void onEvent(EventPayload eventPayload) {
        if (Events.PRE_DEPLOYMENT.equals(eventPayload.getEventCode())) {

            AbstractDeployment deployment = eventPayload.getPayload();
            checkModuleActive(deployment);
            checkLogTracing(deployment);
        }
        if (Events.EXECUTION.equals(eventPayload.getEventCode())) {
            // Events.EXECUTION Only triggered when running within Jakarta Runner
            // And module is explicitly requested, so it is fine to set it active
            // Needed since no Sniffers are running and thus @LoginConfig is not detected
            AbstractDeployment deployment = eventPayload.getPayload();
            deployment.addDeploymentData(MPJWTModuleConstant.JWTAUTH_ENABLED, "True");
        }

    }

    private void checkLogTracing(AbstractDeployment deployment) {

        boolean tracingActive = Boolean.parseBoolean(deployment.getDeploymentData("jwt-auth.tracing.active"));
        LogTracingHelper.getInstance().storeLogTracingActive(deployment.getContextRoot(), tracingActive);
    }

    private void checkModuleActive(AbstractDeployment deployment) {
        boolean moduleActive = Boolean.parseBoolean(deployment.getDeploymentData(MPJWTModuleConstant.JWTAUTH_ENABLED));
        if (moduleActive) {
            return;  // Already active, no need to check
        }
        String realmName = deployment.getDeploymentData(MPJWTModuleConstant.REALM_NAME);
        if (realmName != null) {
            moduleActive = true;

            addPackageWithProvider(deployment);
        }
        deployment.addDeploymentData(MPJWTModuleConstant.JWTAUTH_ENABLED, Boolean.toString(moduleActive));
    }

    private void addPackageWithProvider(AbstractDeployment deployment) {
        ExtraPackagesUtil.addPackages(deployment, "be.atbash.runtime.security.jwt.jaxrs");
    }

    @Override
    public Class<RuntimeConfiguration> getModuleConfigClass() {
        return RuntimeConfiguration.class;
    }

    @Override
    public void setConfig(RuntimeConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void run() {
        // We force that MPConfig module is always active .
        configuration.getConfig().getModules().writeConfigValue(MP_CONFIG_MODULE_NAME, ENABLED_FORCED, "true");

    }


}
