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
package be.atbash.runtime.core.data.version;

public final class VersionInfo {

    public static final VersionInfo INSTANCE = new VersionInfo();

    private VersionReader versionReader;

    private VersionInfo() {
        versionReader = new VersionReader("runtime-main");
    }

    public String getReleaseVersion() {
        return versionReader.getReleaseVersion();
    }

    public String getBuildTime() {
        return versionReader.getBuildTime();
    }

    public static VersionInfo getInstance() {
        return INSTANCE;
    }

}
