/*
 * Copyright 2021-2023 Rudy De Busscher (https://www.atbash.be)
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

public enum DeploymentPhase {

    // @formatter:off
    NOT_STARTED(false, false, false, false, false)
    , VERIFIED(true, false, false, false, false)
    , PREPARED(true, true, false, false, false)
    , DEPLOYED(true, true, true, false, false)
    , FAILED(true, true, false, true, false)
    , READY(true, true, true, false, true);
    // @formatter:on

    private final boolean verified;
    private final boolean prepared;
    private final boolean deployed;
    private final boolean failed;
    private final boolean ready;
    DeploymentPhase(boolean verified, boolean prepared, boolean deployed, boolean failed, boolean ready) {
        this.verified = verified;
        this.prepared = prepared;
        this.deployed = deployed;
        this.failed = failed;
        this.ready = ready;
    }

    /**
     * Verified means the deploymentLocation points to a valid expanded war
     *  archiveFile is specified. An application executed by the runner is immediately
     *  in verified status.
     * @return true when deployment is verified.
     */
    public boolean isVerified() {
        return verified;
    }

    /**
     * Prepared means that ALL preparation is done. (defined DeploymentModule, specifications, ...)
     * @return true when all preparation is done.
     */
    public boolean isPrepared() {
        return prepared;
    }

    /**
     * Deployment is deployed, it might not be ready yet to accept requests. (deployment is in progress)
     * @return true when deployed.
     */
    public boolean isDeployed() {
        return deployed;
    }

    /**
     * Deployment was tried but it failed. See {@link be.atbash.runtime.core.data.deployment.AbstractDeployment#deploymentException}
     * for the reason.
     * @return true when deployment failed.
     */
    public boolean isFailed() {
        return failed;
    }

    /**
     * Deployment is deployed and ready for accepting requests.
     * @return true is ready for accepting requests.
     */
    public boolean isReady() {
        return ready;
    }
}
