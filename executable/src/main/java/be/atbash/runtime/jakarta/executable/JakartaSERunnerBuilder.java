package be.atbash.runtime.jakarta.executable;

import jakarta.ws.rs.core.Application;

import java.util.ServiceLoader;

public final class JakartaSERunnerBuilder {

    private final JakartaRunnerData runnerData = new JakartaRunnerData();

    private JakartaSERunnerBuilder(Class<? extends Application> application) {
        runnerData.addResources(application);
    }

    private JakartaSERunnerBuilder(Class<?>... resourceClasses) {
        runnerData.addResources(resourceClasses);
    }

    public JakartaSERunnerBuilder withPort(int port) {
        runnerData.setPort(port);
        return this;
    }

    public JakartaSERunnerBuilder withHost(String host) {
        runnerData.setHost(host);
        return this;
    }

    public void run() {
        getRunner().start(runnerData);
    }

    private JakartaRunner getRunner() {
        ServiceLoader<JakartaRunner> loader = ServiceLoader.load(JakartaRunner.class);
        return loader.findFirst().orElseThrow(JakartaRunnerNotFoundException::new);
    }

    public static JakartaSERunnerBuilder newBuilder(Class<? extends Application> application) {
        return new JakartaSERunnerBuilder(application);
    }

    public static JakartaSERunnerBuilder newBuilder(Class<?>... resourceClasses) {
        return new JakartaSERunnerBuilder(resourceClasses);
    }
}
