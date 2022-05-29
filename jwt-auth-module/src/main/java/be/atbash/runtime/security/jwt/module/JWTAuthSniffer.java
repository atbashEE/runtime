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

import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.security.jwt.MPJWTModuleConstant;
import org.eclipse.microprofile.auth.LoginConfig;

import java.lang.annotation.Annotation;
import java.util.Map;

public class JWTAuthSniffer implements Sniffer {

    private String realmName;

    @Override
    public Specification[] detectedSpecifications() {
        return new Specification[0];
    }

    @Override
    public boolean triggered(Class<?> aClass) {
        boolean triggered = false;
        for (Annotation annotation : aClass.getAnnotations()) {
            if (LoginConfig.class.isAssignableFrom(annotation.annotationType())) {
                //  We should check Auth-method member but TCK itself doesn't perform this since
                //  @LoginConfig is MP JWT specific.
                triggered = true;
                LoginConfig loginConfig = (LoginConfig) annotation;
                realmName = loginConfig.realmName();
            }
        }
        return triggered;
    }

    @Override
    public boolean triggered(String descriptorName, String content) {
        return false;
    }

    @Override
    public boolean isFastDetection() {
        return true;
    }

    @Override
    public Map<String, String> deploymentData() {
        return Map.of(MPJWTModuleConstant.REALM_NAME, realmName);
    }
}
