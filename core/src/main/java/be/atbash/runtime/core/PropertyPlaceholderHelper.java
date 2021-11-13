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
package be.atbash.runtime.core;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * // Based on Glassfish code
 */
public class PropertyPlaceholderHelper {

    public final static String ENV_REGEX = "([^\\$]*)\\$\\{ENV=([^\\}]*)\\}([^\\$]*)";
    private final Pattern pattern;

    private final Map<String, String> properties;

    private final int MAX_SUBSTITUTION_DEPTH = 100;

    public PropertyPlaceholderHelper(Map<String, String> properties, String regex) {
        this.properties = properties;
        this.pattern = Pattern.compile(regex);
    }

    public String getPropertyValue(String key) {
        return properties.get(key);
    }

    public Properties replacePropertiesPlaceholder(Properties properties) {
        final Properties p = new Properties();
        Set<String> keys = properties.stringPropertyNames();

        for (String key : keys) {
            p.setProperty(key, replacePlaceholder(properties.getProperty(key)));
        }
        return p;
    }

    public String replacePlaceholder(String value) {
        if (value != null && value.indexOf('$') != -1) {
            String origValue = value;
            int i = 0;
            // Perform Environment variable substitution
            Matcher m = getPattern().matcher(value);

            while (m.find() && i < MAX_SUBSTITUTION_DEPTH) {
                String matchValue = m.group(2).trim();
                String newValue = getPropertyValue(matchValue);
                if (newValue != null) {
                    value = m.replaceFirst(Matcher.quoteReplacement(m.group(1) + newValue + m.group(3)));
                    m.reset(value);
                }
                i++;
            }

            if (i >= MAX_SUBSTITUTION_DEPTH) {
                Logger.getLogger(PropertyPlaceholderHelper.class.getName()).log(Level.SEVERE, "System property substitution exceeded maximum of {0}", MAX_SUBSTITUTION_DEPTH);
            }
        }
        return value;
    }

    public Pattern getPattern() {
        return pattern;
    }
}
