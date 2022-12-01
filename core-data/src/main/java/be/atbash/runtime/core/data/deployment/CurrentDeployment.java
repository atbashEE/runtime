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
package be.atbash.runtime.core.data.deployment;

public class CurrentDeployment {

    private static final CurrentDeployment INSTANCE = new CurrentDeployment();

    private final ThreadLocal<AbstractDeployment> threadLocalValue = new ThreadLocal<>();

    // For the Jakarta runner and Jersey SE, we have async startup (different threads)
    // But Jakarta runner can only run 1, so we keep it here also in 'global'
    // FIXME review, do we really need ThreadLocal for multiple concurrent deployments?
    private AbstractDeployment currentDeployment;

    public void setCurrent(AbstractDeployment deployment) {
        threadLocalValue.set(deployment);
        currentDeployment = deployment;
    }

    public AbstractDeployment getCurrent() {
        AbstractDeployment deployment = threadLocalValue.get();
        if (deployment == null) {
            deployment = currentDeployment;
        }
        return deployment;
    }

    public void clear() {
        threadLocalValue.remove();
        currentDeployment = null;
    }

    public static CurrentDeployment getInstance() {
        return INSTANCE;
    }
}
