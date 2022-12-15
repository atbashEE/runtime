package be.atbash.runtime.core.data.deployment;

import be.atbash.runtime.core.data.module.Module;

import java.io.File;
import java.util.Map;

public class AbstractDeployment {

    private final String deploymentName;

    private Module<?> deploymentModule;
    private final Map<String, String> deploymentData;

    private File configDataFile;

    private Exception deploymentException;

    public AbstractDeployment(String deploymentName, Map<String, String> deploymentData) {
        this.deploymentName = deploymentName;
        this.deploymentData = deploymentData;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public Module<?> getDeploymentModule() {
        return deploymentModule;
    }

    public void setDeploymentModule(Module<?> deploymentModule) {
        this.deploymentModule = deploymentModule;
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


    public Exception getDeploymentException() {
        return deploymentException;
    }

    public void setDeploymentException(Exception deploymentException) {
        this.deploymentException = deploymentException;
    }

    public boolean hasDeploymentFailed() {
        return getDeploymentException() != null;
    }

}
