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

import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.util.StringUtil;
import be.atbash.runtime.logging.mapping.BundleMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

public abstract class AbstractDeployment {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String deploymentName;

    private String contextRoot;

    private Module<?> deploymentModule;

    private final Map<String, String> deploymentData;

    private File configDataFile;

    private Exception deploymentException;

    protected DeploymentPhase deploymentPhase;

    static {
        BundleMapping.getInstance().addMapping(ArchiveDeployment.class.getName(), AbstractDeployment.class.getName());
        BundleMapping.getInstance().addMapping(ApplicationExecution.class.getName(), AbstractDeployment.class.getName());
    }

    // We had some issue at some point to correctly indicate within HealthHandler
    // if application was ready and could handle requests.
    // BUt we now have the case that when JettyModule add and start the Handler,
    // the statement only returns after the applicationListener says it is up.
    // So the DeploymentPhase is not yet set to Deployed and thus some logic is messed up.
    // deployInitiated indicates that handler is about to be started, and that applicationReady
    // check on deployed or deployInitiated equals to true
    // and we don't blindly set phase deployed.
    private boolean deployInitiated;

    public AbstractDeployment(String deploymentName, String contextRoot, Map<String, String> deploymentData) {
        deploymentPhase = DeploymentPhase.NOT_STARTED;
        this.deploymentName = deploymentName;
        this.contextRoot = contextRoot;
        this.deploymentData = deploymentData;
    }

    public DeploymentPhase getDeploymentPhase() {
        return deploymentPhase;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public String getContextRoot() {
        return contextRoot == null ? "/" + getDeploymentName() : contextRoot;
    }

    public void setContextRoot(String contextRoot) {
        if (!deploymentPhase.isDeployed() && !deploymentPhase.isFailed()
                && !(deploymentPhase.isPrepared() && deployInitiated)) {
            this.contextRoot = StringUtil.sanitizePath(contextRoot);
        } else {
            logger.atError().addArgument(() -> deploymentName).log("DEPLOY-110");
        }
    }

    public Module<?> getDeploymentModule() {
        return deploymentModule;
    }

    public void setDeploymentModule(Module<?> deploymentModule) {
        this.deploymentModule = deploymentModule;
        // Deployment module is the last thing we determine before we can consider the deployment
        // to be ready.
        if (getDeploymentPhase().isVerified() && deploymentModule != null) {
            deploymentPhase = DeploymentPhase.PREPARED;
        }

    }

    public Map<String, String> getDeploymentData() {
        return deploymentData;
    }

    public String getDeploymentData(String key) {
        return deploymentData.get(key);
    }

    public void addDeploymentData(String key, String value) {
        deploymentData.put(key, value);
    }

    public File getConfigDataFile() {
        return configDataFile;
    }

    public void setConfigDataFile(File configDataFile) {
        this.configDataFile = configDataFile;
    }

    public void setDeployInitiated() {
        deployInitiated = true;
    }

    public void setDeployed() {
        if (!deploymentPhase.isReady() && !deploymentPhase.isFailed()) {
            // We are not yet ready with the app, so DEPLOYED makes sense.
            deploymentPhase = DeploymentPhase.DEPLOYED;
        }
    }

    public Exception getDeploymentException() {
        return deploymentException;
    }

    public void setDeploymentException(Exception deploymentException) {
        this.deploymentException = deploymentException;
        deploymentPhase = DeploymentPhase.FAILED;
    }

    public boolean hasDeploymentFailed() {
        // Convenient
        return getDeploymentPhase().isFailed();
    }

    public void setApplicationReady() {

        if (deploymentPhase == DeploymentPhase.DEPLOYED ||
                (deploymentPhase == DeploymentPhase.PREPARED && deployInitiated)) {
            deploymentPhase = DeploymentPhase.READY;
        } else {
            logger.atError().addArgument(() -> deploymentName).log("DEPLOY-111");
        }
    }

    // archiveDeployments are identified by context root.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbstractDeployment)) {
            return false;
        }

        AbstractDeployment that = (AbstractDeployment) o;

        return contextRoot.equals(that.contextRoot);
    }

    @Override
    public int hashCode() {
        return contextRoot.hashCode();
    }
}
