package be.atbash.runtime.jakarta.executable.testclasses;

import be.atbash.runtime.jakarta.executable.JakartaRunner;
import be.atbash.runtime.jakarta.executable.JakartaRunnerData;

public class TestRunner implements JakartaRunner {
    public static JakartaRunnerData jakartaRunnerData;

    @Override
    public void start(JakartaRunnerData runnerData) {
        jakartaRunnerData = runnerData;
    }
}
