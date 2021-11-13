/*
 * Copyright 2021 Rudy De Busscher (https://www.atbash.be)
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
package be.atbash.runtime.config;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

class ConfigInstanceUtilTest {

    private static final File TEST_DIRECTORY = new File(".");
    private static final File NONEXISTING_DIRECTORY = System.getProperty("os.name").toLowerCase().contains("win")
            ? new File("C:/does/not/exist") : new File("/does/not/exist");

    private ByteArrayOutputStream outCapture;
    private PrintStream oldOut;

    @BeforeEach
    public void redirectOutput() {
        oldOut = System.out;

        outCapture = new ByteArrayOutputStream();
        PrintStream newOut = new PrintStream(outCapture);

        System.setOut(newOut);
    }

    @AfterEach
    public void captureOutputAndRestoreOutput() {
        System.out.flush();
        System.setOut(oldOut);
    }

    @Test
    public void testExistingConfigName() {

        ConfigInstance configInstance = new ConfigInstance(TEST_DIRECTORY.getAbsolutePath(), "testconfig", false, true);
        ConfigInstanceUtil.processConfigInstance(configInstance);

        Assertions.assertThat(outCapture.toString()).isEqualTo("CI-004: The config name 'testconfig' already exists.\n");
        Assertions.assertThat(configInstance.isValid()).isTrue();
        Assertions.assertThat(configInstance.isExistingConfigDirectory()).isTrue();
    }

    @Test
    public void testNonExistingDirectory() {

        ConfigInstance configInstance = new ConfigInstance(NONEXISTING_DIRECTORY.getAbsolutePath(), "testconfig", false, true);
        ConfigInstanceUtil.processConfigInstance(configInstance);

        Assertions.assertThat(outCapture.toString()).isEqualTo("CI-001: The specified root directory '/does/not/exist' doesn't point to an existing directory\n");
        Assertions.assertThat(configInstance.isValid()).isFalse();
    }

    @Test
    public void testWithFile() {

        ConfigInstance configInstance = new ConfigInstance(new File(TEST_DIRECTORY, "pom.xml").getAbsolutePath(), "testconfig", false, true);
        ConfigInstanceUtil.processConfigInstance(configInstance);

        String consoleOutput = outCapture.toString();
        Assertions.assertThat(consoleOutput).contains("CI-002: The specified root directory");
        Assertions.assertThat(consoleOutput).contains("pom.xml' is not a directory");
        Assertions.assertThat(configInstance.isValid()).isFalse();
    }

    @Test
    public void test_ProcessConfigInstance_HappyCase() {
        // This test fails when you run it a second time without clearing the target directory.
        ConfigInstance configInstance = new ConfigInstance(new File(TEST_DIRECTORY, "target/").getAbsolutePath(), "testconfig", false, true);
        ConfigInstanceUtil.processConfigInstance(configInstance);

        Assertions.assertThat(outCapture.toString()).isBlank();
        Assertions.assertThat(configInstance.isValid()).isTrue();
        Assertions.assertThat(configInstance.isExistingConfigDirectory()).isFalse();
    }

    @Test
    public void testExistingConfigName_UseConfig() {

        ConfigInstance configInstance = new ConfigInstance(TEST_DIRECTORY.getAbsolutePath(), "testconfig", false, false);
        ConfigInstanceUtil.processConfigInstance(configInstance);

        Assertions.assertThat(outCapture.toString()).isBlank();
        Assertions.assertThat(configInstance.isValid()).isTrue();
        Assertions.assertThat(configInstance.isExistingConfigDirectory()).isTrue();
    }


    @Test
    public void test_storeConfig_HappyCase() {
        // This test fails when you run it a second time without clearing the target directory.
        File testDirectory = new File(TEST_DIRECTORY, "target/");
        ConfigInstance configInstance = new ConfigInstance(testDirectory.getAbsolutePath(), "test", false, true);
        ConfigInstanceUtil.processConfigInstance(configInstance);

        ConfigInstanceUtil.storeRuntimeConfig(configInstance);
        Assertions.assertThat(new File(testDirectory, "test/config.json")).exists();
    }
}