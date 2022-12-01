package be.atbash.runtime.core.data.deployment;

import java.util.HashMap;
import java.util.List;

public class ApplicationExecution extends AbstractDeployment {

    private final List<Class<?>> resources;

    private int port;

    private String host;

    public ApplicationExecution(List<Class<?>> resources) {
        super("Jakarta Core profile application", new HashMap<>());
        this.resources = resources;
    }

    public List<Class<?>> getResources() {
        return resources;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
