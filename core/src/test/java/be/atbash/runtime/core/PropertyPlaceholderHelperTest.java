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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PropertyPlaceholderHelperTest {
    private Map<String, String> env = new HashMap<>();

    @BeforeEach
    public void setUp() {
        env.put("testEnvironmentHandlers", "java.util.logging.ConsoleHandler");
        env.put("testEnvironmentHandlerServices", "com.sun.enterprise.server.logging.GFFileHandler,com.sun.enterprise.server.logging.SyslogHandler");
    }

    @AfterEach
    public void tearDown() {
        env = null;
    }

    /**
     * Test of getPropertyValue method, of class PropertyPlaceholderHelper.
     */
    @Test
    public void testGetPropertyValueFromEnv() {
        System.out.println("getPropertyValueFromEnv");
        String loggingProperty1 = "testEnvironmentHandlers";
        String loggingProperty2 = "testEnvironmentHandlerServices";

        PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper(env, PropertyPlaceholderHelper.ENV_REGEX);
        String expResult1 = "java.util.logging.ConsoleHandler";
        String result1 = propertyPlaceholderHelper.getPropertyValue(loggingProperty1);
        assertEquals(expResult1, result1);

        propertyPlaceholderHelper = new PropertyPlaceholderHelper(env, PropertyPlaceholderHelper.ENV_REGEX);
        String expResult2 = "com.sun.enterprise.server.logging.GFFileHandler,com.sun.enterprise.server.logging.SyslogHandler";
        String result2 = propertyPlaceholderHelper.getPropertyValue(loggingProperty2);
        assertEquals(expResult2, result2);
    }

    /**
     * Test of replacePropertiesPlaceholder method, of class
     */
    @Test
    public void testReplacePropertiesPlaceholder() {
        System.out.println("replacePropertiesPlaceholder");
        Properties props = new Properties();
        props.setProperty("testEnvironmentHandlersProperties", "${ENV=testEnvironmentHandlers}");
        props.setProperty("testEnvironmentHandlerServicesProperties", "${ENV=testEnvironmentHandlerServices}");

        Properties expResultProps = new Properties();
        expResultProps.setProperty("testEnvironmentHandlersProperties", "java.util.logging.ConsoleHandler");
        expResultProps.setProperty("testEnvironmentHandlerServicesProperties", "com.sun.enterprise.server.logging.GFFileHandler,com.sun.enterprise.server.logging.SyslogHandler");

        PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper(env, PropertyPlaceholderHelper.ENV_REGEX);
        Properties result = propertyPlaceholderHelper.replacePropertiesPlaceholder(props);

        assertEquals(expResultProps.getProperty("testEnvironmentHandlersProperties"), result.getProperty("testEnvironmentHandlersProperties"));
        assertEquals(expResultProps.getProperty("testEnvironmentHandlerServicesProperties"), result.getProperty("testEnvironmentHandlerServicesProperties"));
    }

}