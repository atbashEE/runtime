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
package be.atbash.runtime.security.jwt.inject;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Override the default CDI Principal bean to allow the injection of a Principal to be a JsonWebToken
 */
@Priority(1)
@RequestScoped
public class PrincipalProducer {
    private JsonWebToken token;

    public void setJsonWebToken(JsonWebToken token) {
        this.token = token;
    }

    /**
     * The producer method for the current JsonWebToken
     *
     * @return JsonWebToken
     */
    @Produces
    @RequestScoped
    JsonWebToken currentJWTPrincipalOrNull() {
        return token == null ? new NullJsonWebToken() : token;
    }
}