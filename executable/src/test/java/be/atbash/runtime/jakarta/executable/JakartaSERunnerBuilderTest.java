package be.atbash.runtime.jakarta.executable;

import be.atbash.runtime.jakarta.executable.testclasses.TestApplication;
import be.atbash.runtime.jakarta.executable.testclasses.TestRunner;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class JakartaSERunnerBuilderTest {

    @Test
    void newBuilder_withApplication() {
        JakartaSERunnerBuilder.newBuilder(TestApplication.class)
                .withPort(8888)
                .run();

        Assertions.assertThat(TestRunner.jakartaRunnerData).isNotNull();
        Assertions.assertThat(TestRunner.jakartaRunnerData.getPort()).isEqualTo(8888);
        Assertions.assertThat(TestRunner.jakartaRunnerData.getResources()).containsOnly(TestApplication.class);
    }
}