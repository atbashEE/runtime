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
package be.atbash.runtime.security.jwt;

import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.deployment.CurrentArchiveDeployment;
import be.atbash.runtime.security.jwt.cdi.ValidateJWTConfiguration;
import be.atbash.runtime.security.jwt.inject.*;
import be.atbash.runtime.security.jwt.principal.JWTCallerPrincipalFactory;
import be.atbash.runtime.security.jwt.principal.RuntimeKeyManager;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AuthExtension implements Extension {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthExtension.class);

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery event, BeanManager beanManager) {
        ArchiveDeployment archiveDeployment = CurrentArchiveDeployment.getInstance().getCurrent();
        boolean moduleActive = Boolean.parseBoolean(archiveDeployment.getDeploymentData(MPJWTModuleConstant.JWTAUTH_ENABLED));

        if (!moduleActive) {
            return;
        }

        LOGGER.atInfo()
                .addArgument(archiveDeployment.getDeploymentName())
                .log("JWT-100");

        addAnnotatedType(event, beanManager, ClaimValueProducer.class);
        addAnnotatedType(event, beanManager, CommonJwtProducer.class);
        addAnnotatedType(event, beanManager, JWTCallerPrincipalFactory.class);
        addAnnotatedType(event, beanManager, RuntimeKeyManager.class);
        addAnnotatedType(event, beanManager, JsonValueProducer.class);
        addAnnotatedType(event, beanManager, JWTAuthContextInfoProvider.class);
        addAnnotatedType(event, beanManager, PrincipalProducer.class);
        addAnnotatedType(event, beanManager, RawClaimTypeProducer.class);
        addAnnotatedType(event, beanManager, ValidateJWTConfiguration.class);

    }

    void addAnnotatedType(BeforeBeanDiscovery event, BeanManager beanManager, Class<?> type) {
        String id = "JWT" + type.getSimpleName();
        event.addAnnotatedType(beanManager.createAnnotatedType(type), id);
    }

}
