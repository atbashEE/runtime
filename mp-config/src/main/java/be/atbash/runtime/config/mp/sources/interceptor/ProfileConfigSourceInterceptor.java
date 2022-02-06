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
package be.atbash.runtime.config.mp.sources.interceptor;

import be.atbash.runtime.config.mp.AtbashConfig;
import be.atbash.runtime.config.mp.ConfigValueImpl;
import be.atbash.runtime.config.mp.converter.Converters;
import org.eclipse.microprofile.config.ConfigValue;

import java.util.*;


/**
 * A {@link ConfigSourceInterceptor} that handles profiles.  When created, it looks for the profile configuration value
 */
public class ProfileConfigSourceInterceptor implements ConfigSourceInterceptor {
    private final String[] profiles;


    public ProfileConfigSourceInterceptor(List<String> profiles) {
        List<String> reverseProfiles = new ArrayList<>(profiles);
        Collections.reverse(reverseProfiles);
        this.profiles = reverseProfiles.toArray(new String[0]);
    }

    public ProfileConfigSourceInterceptor(ConfigSourceInterceptorContext context) {
        this(convertProfile(context));
    }

    @Override
    public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
        if (profiles.length > 0) {
            // Make sure we are using a name that doesn't have any profile prefix value.
            String normalizedName = normalizeName(name);
            ConfigValue profileValue = getProfileValue(context, normalizedName);
            if (profileValue != null) {


                if (profileValue instanceof ConfigValueImpl) {
                    return ((ConfigValueImpl) profileValue).withName(normalizedName);
                }

            }
        }

        return context.proceed(name);
    }

    public ConfigValue getProfileValue(ConfigSourceInterceptorContext context, String normalizeName) {
        for (String profile : profiles) {
            ConfigValue profileValue = context.proceed("%" + profile + "." + normalizeName);
            if (profileValue != null) {
                if (profileValue instanceof ConfigValueImpl) {
                    return ((ConfigValueImpl) profileValue).withProfile(profile);
                }
            }
        }

        return null;
    }

    @Override
    public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
        final Set<String> names = new HashSet<>();
        final Iterator<String> namesIterator = context.iterateNames();
        while (namesIterator.hasNext()) {
            names.add(normalizeName(namesIterator.next()));
        }
        return names.iterator();
    }


    public String[] getProfiles() {
        return profiles;
    }

    private String normalizeName(String name) {
        for (String profile : profiles) {
            if (name.startsWith("%" + profile + ".")) {
                return name.substring(profile.length() + 2);
            }
        }

        return name;
    }

    private static List<String> convertProfile(String profile) {
        return Converters.newCollectionConverter(Converters.newTrimmingConverter(Converters.STRING_CONVERTER), ArrayList::new).convert(profile);
    }

    private static List<String> convertProfile(ConfigSourceInterceptorContext context) {
        List<String> profiles = new ArrayList<>();
        ConfigValue profile = context.proceed(AtbashConfig.CONFIG_PROFILE_KEY);
        if (profile != null) {
            List<String> convertedProfiles = convertProfile(profile.getValue());
            if (convertedProfiles != null) {
                profiles.addAll(convertedProfiles);
            }
        }
        return profiles;
    }
}
