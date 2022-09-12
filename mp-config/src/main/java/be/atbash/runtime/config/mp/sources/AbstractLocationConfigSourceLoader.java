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
package be.atbash.runtime.config.mp.sources;

import be.atbash.runtime.core.data.util.ResourceReader;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This {@link AbstractLocationConfigSourceLoader} loads {@link ConfigSource}s from a list of specific
 * locations.
 * <p>
 * <p>
 * The locations comprise a list of valid {@link URI}s which are loaded in order. The following URI schemes are
 * supported:
 *
 * <ol>
 * <li>file or directory</li>
 * <li>classpath resource</li>
 * <li>jar resource</li>
 * <li>http resource</li>
 * </ol>
 * <p>
 * <p>
 * If a profile is active, the profile resource is only loaded if the unprofiled resource is available in the same
 * location. This is to keep a consistent loading order and match with the unprofiled resource. Profiles are not
 * taken into account if the location is a directory.
 * <p>
 * Based on code from SmallRye Config.
 */
public abstract class AbstractLocationConfigSourceLoader {

    /**
     * Loads a {@link ConfigSource} from an {@link URL}. Implementations must construct the {@link ConfigSource} to
     * load.
     *
     * @param url     the {@link URL} to load the {@link ConfigSource}.
     * @param ordinal the ordinal of the {@link ConfigSource}.
     * @return the loaded {@link ConfigSource}.
     * @throws IOException if an error occurred when reading from the the {@link URL}.
     */
    protected abstract ConfigSource loadConfigSource(URL url, int ordinal) throws IOException;

    protected List<ConfigSource> loadConfigSources(String[] locations, int ordinal) {
        if (locations == null || locations.length == 0) {
            return Collections.emptyList();
        }

        List<ConfigSource> configSources = new ArrayList<>();
        for (String location : locations) {
            List<URI> resources = ResourceReader.getResources(location);
            for (URI resource : resources) {
                ConfigSource mainSource = addConfigSource(resource, ordinal, configSources);
                configSources.addAll(tryProfiles(resource, mainSource));
            }
        }
        return configSources;
    }

    protected List<ConfigSource> tryProfiles(URI uri, ConfigSource mainSource) {
        List<ConfigSource> configSources = new ArrayList<>();
        configSources.add(new ConfigurableConfigSource((ProfileConfigSourceFactory) profiles -> {
            List<ConfigSource> profileSources = new ArrayList<>();
            for (int i = profiles.size() - 1; i >= 0; i--) {
                int ordinal = mainSource.getOrdinal() + profiles.size() - i;
                URI profileUri = addProfileName(uri, profiles.get(i));
                addProfileConfigSource(toURL(profileUri), ordinal, profileSources);
            }
            return profileSources;
        }));
        return configSources;
    }

    private static URL toURL(URI uri) {
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private ConfigSource addConfigSource(URI uri, int ordinal, List<ConfigSource> configSources) {
        return addConfigSource(toURL(uri), ordinal, configSources);
    }

    private ConfigSource addConfigSource(URL url, int ordinal, List<ConfigSource> configSources) {
        try {
            ConfigSource configSource = loadConfigSource(url, ordinal);
            configSources.add(configSource);
            return configSource;
        } catch (IOException e) {

            throw new IllegalStateException(String.format("Failed to load resource %s", url));

        }
    }

    private void addProfileConfigSource(URL profileToFileName, int ordinal,
                                        List<ConfigSource> profileSources) {
        try {
            profileSources.add(loadConfigSource(profileToFileName, ordinal));
        } catch (FileNotFoundException | NoSuchFileException e) {
            // It is ok to not find the resource here, because it is an optional profile resource.
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load resource %s", e);
        }
    }

    private static URI addProfileName(URI uri, String profile) {
        if ("jar".equals(uri.getScheme())) {
            return URI.create("jar:" + addProfileName(URI.create(uri.getRawSchemeSpecificPart()), profile));
        }

        String fileName = uri.getPath();
        assert fileName != null;

        int dot = fileName.lastIndexOf(".");
        String fileNameProfile;
        if (dot != -1 && dot != 0 && fileName.charAt(dot - 1) != '/') {
            fileNameProfile = fileName.substring(0, dot) + "-" + profile + fileName.substring(dot);
        } else {
            fileNameProfile = fileName + "-" + profile;
        }

        try {
            return new URI(uri.getScheme(),
                    uri.getAuthority(),
                    uri.getHost(),
                    uri.getPort(),
                    fileNameProfile,
                    uri.getQuery(),
                    uri.getFragment());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    interface ProfileConfigSourceFactory extends ConfigSourceFactory {
        @Override
        default Iterable<ConfigSource> getConfigSources(ConfigSourceContext context) {
            List<String> profiles = context.getProfiles();
            if (profiles.isEmpty()) {
                return Collections.emptyList();
            }

            return getProfileConfigSources(profiles);
        }

        Iterable<ConfigSource> getProfileConfigSources(List<String> profiles);
    }
}
