/*
 * Copyright 2021-2022 Rudy De Busscher (https://www.atbash.be)
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
package be.atbash.runtime.testing.model;

import be.atbash.runtime.testing.config.Config;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ServerAdapterMetaData {

    private RuntimeType runtimeType;  // Default or Custom
    private String customImageName;  // In case of Custom, the name of the directory containing the Docker build script and contents for the image.
    private String runtimeVersion;
    private JDKRuntime jdkRuntime;
    private boolean testApplication;
    private boolean testStartupFailure;
    private String[] startupParameters;
    private boolean debugMode;

    private Map<String, String> volumeMappings;

    private ServerAdapterMetaData() {
    }

    public RuntimeType getRuntimeType() {
        return runtimeType;
    }

    public String getRuntimeVersion() {
        return runtimeVersion;
    }

    public String getCustomImageName() {
        return customImageName;
    }

    public void setCustomImageName(String customImageName) {
        this.customImageName = customImageName;
    }

    public JDKRuntime getJdkRuntime() {
        return jdkRuntime;
    }

    public boolean isTestApplication() {
        return testApplication;
    }

    public void setTestApplication(boolean testApplication) {
        this.testApplication = testApplication;
    }

    public boolean isTestStartupFailure() {
        return testStartupFailure;
    }

    public void setTestStartupFailure(boolean testStartupFailure) {
        this.testStartupFailure = testStartupFailure;
    }

    public Map<String, String> getVolumeMappings() {
        return volumeMappings;
    }

    public void setVolumeMappings(Map<String, String> volumeMappings) {
        this.volumeMappings = volumeMappings;
    }

    public static ServerAdapterMetaData parse(String data) {
        ServerAdapterMetaData result = new ServerAdapterMetaData();

        String[] parts = handlePredefinedValues(data).split("-");

        if (!parts[0].trim().isEmpty()) {
            result.runtimeType = RuntimeType.parse(parts[0].trim());
        } else {
            result.runtimeType = RuntimeType.DEFAULT;  // When first part is empty, we assume DEFAULT
        }
        if (result.runtimeType == RuntimeType.CUSTOM) {
            result.setCustomImageName(parts[0].trim());
        }

        if (parts.length > 1) {
            result.runtimeVersion = restorePredefinedValues(parts[1].trim());
        }
        if (parts.length > 2) {
            result.jdkRuntime = JDKRuntime.parse(parts[2].trim());
            if (result.jdkRuntime == JDKRuntime.UNKNOWN) {
                System.err.println("Unknown JDKRuntime definition :" + parts[2].trim());
                result.jdkRuntime = null;
            }

        }

        if (result.runtimeVersion == null || result.runtimeVersion.isEmpty()) {
            result.runtimeVersion = Config.getVersion();
        }

        if (result.jdkRuntime == null) {
            result.jdkRuntime = Config.getJDKRuntime();
        }

        return result;
    }

    private static String restorePredefinedValues(String data) {
        return data.replaceAll("_SNAPSHOT", "-SNAPSHOT").replaceAll("_RC", "-RC");
    }

    private static String handlePredefinedValues(String data) {
        // The version can have -SNAPSHOT or -RC.
        return data.replaceAll("-SNAPSHOT", "_SNAPSHOT").replaceAll("-RC", "_RC");
    }

    public void setStartupParameters(String[] startupParameters) {
        this.startupParameters = startupParameters;
    }

    public List<String> getStartupParameters() {
        return Arrays.asList(startupParameters);
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public boolean isDebugMode() {
        return debugMode;
    }
}
