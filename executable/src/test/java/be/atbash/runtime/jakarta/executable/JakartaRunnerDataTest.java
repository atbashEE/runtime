package be.atbash.runtime.jakarta.executable;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class JakartaRunnerDataTest {

    @Test
    void getResources() {
        JakartaRunnerData runnerData = new JakartaRunnerData();
        runnerData.addResources(String.class, JakartaRunner.class);

        Assertions.assertThat(runnerData.getResources()).containsOnly(String.class, JakartaRunner.class);
    }

    @Test
    void getPort_defaultValue() {
        JakartaRunnerData runnerData = new JakartaRunnerData();
        Assertions.assertThat(runnerData.getPort()).isEqualTo(8080);
    }

    @Test
    void setPort() {
        JakartaRunnerData runnerData = new JakartaRunnerData();
        runnerData.setPort(8888);
        Assertions.assertThat(runnerData.getPort()).isEqualTo(8888);
    }

    @Test
    void getHost_defaultValue() {
        JakartaRunnerData runnerData = new JakartaRunnerData();
        Assertions.assertThat(runnerData.getHost()).isEqualTo("localhost");
    }

    @Test
    void setHost() {
        JakartaRunnerData runnerData = new JakartaRunnerData();
        runnerData.setHost("my-server");
        Assertions.assertThat(runnerData.getHost()).isEqualTo("my-server");
    }
}