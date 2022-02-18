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
package be.atbash.runtime.core.data.version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Reads the information from the Manifest file for a certain module. The initial characters of the jar file containing the manifest file
 * like 'jwk-util' or 'octopus-core' needs to be passed as parameter.
 */
public class VersionReader {

    private final Logger logger = LoggerFactory.getLogger(VersionReader.class);

    private String releaseVersion;
    private String buildTime;

    /**
     * @param module Initial characters of the jar file with the manifest file of interest.
     */
    public VersionReader(String module) {
        readInfo(module);
    }

    private void readInfo(String module) {

        Properties properties = new Properties();
        InputStream resourceAsStream = null;
        try {
            URL manifestFile = findManifestFile(module);

            // Is the manifest file found.
            if (manifestFile != null) {
                resourceAsStream = manifestFile.openStream();
                properties.load(resourceAsStream);

            } else {
                logger.warn(String.format("Unable to find manifest file %s module", module));
            }

        } catch (IOException e) {
            logger.warn(String.format("Exception during loading of the %s MANIFEST.MF file : %s", module, e.getMessage()));
        } finally {
            if (resourceAsStream != null) {
                try {
                    resourceAsStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        releaseVersion = properties.getProperty("Release-Version");
        buildTime = properties.getProperty("buildTime");

    }

    private URL findManifestFile(String module) throws IOException {
        URL moduleResult = null;
        URL firstResult = null;
        ClassLoader classLoader = this.getClass().getClassLoader();
        Enumeration<URL> systemResources = classLoader.getResources("META-INF/MANIFEST.MF");
        while (systemResources.hasMoreElements() && moduleResult == null) {
            URL url = systemResources.nextElement();
            if (firstResult == null) {
                firstResult = url;
            }
            if (url.toExternalForm().contains("/" + module)) {
                moduleResult = url;
            }
        }
        return moduleResult == null ? firstResult : moduleResult;
    }

    /**
     * Returns the <code>Release-Version</code> property of the manifest or null if manifest could not be found.
     *
     * @return Release-Version property value.
     */
    public String getReleaseVersion() {
        return releaseVersion;
    }

    /**
     * Returns the <code>buildTime</code> property of the manifest or null if manifest could not be found.
     *
     * @return buildTime property value
     */
    public String getBuildTime() {
        return buildTime;
    }
}
