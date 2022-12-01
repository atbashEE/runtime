package be.atbash.runtime.jakarta.executable;

import java.util.*;

public class JakartaRunnerData {

    private final List<Class<?>> resources = new ArrayList<>();
    private int port = 8080;

    private String host = "localhost";

    private Map<String, String> applicationData = new HashMap<>();
    private List<String> commandLineEntries = new ArrayList<>();

    public List<Class<?>> getResources() {
        return resources;
    }

    public void addResources(Class<?>... resources) {
        this.resources.addAll(Arrays.asList(resources));
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

    public Map<String, String> getApplicationData() {
        return applicationData;
    }

    public void setApplicationData(Map<String, String> applicationData) {
        this.applicationData = applicationData;
    }

    public List<String> getCommandLineEntries() {
        return commandLineEntries;
    }

    public void setCommandLineEntries(List<String> commandLineEntries) {
        this.commandLineEntries = commandLineEntries;
    }
}
