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
package be.atbash.runtime.config.mp.inject;

import be.atbash.runtime.config.mp.AtbashConfig;
import be.atbash.runtime.config.mp.util.ConfigProducerUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Supplier;

/**
 *
 * CDI producer for {@link Config} bean.
 *
 * Based on code by Jeff Mesnil (c) 2017 Red Hat inc.
 */
@ApplicationScoped
public class ConfigProducer {
    @Produces
    protected AtbashConfig getConfig() {
        return ConfigProvider.getConfig(Thread.currentThread().getContextClassLoader()).unwrap(AtbashConfig.class);
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected String produceStringConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected Long getLongValue(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected Integer getIntegerValue(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected Float produceFloatConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected Double produceDoubleConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected Boolean produceBooleanConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected Short produceShortConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected Byte produceByteConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected Character produceCharacterConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected <T> Optional<T> produceOptionalConfigValue(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected <T> Supplier<T> produceSupplierConfigValue(InjectionPoint ip) {
        return () -> ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected <T> Set<T> producesSetConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected <T> List<T> producesListConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected <K, V> Map<K, V> producesMapConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected OptionalInt produceOptionalIntConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected OptionalLong produceOptionalLongConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected OptionalDouble produceOptionalDoubleConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected ConfigValue produceConfigValue(InjectionPoint ip) {
        return ConfigProducerUtil.getConfigValue(ip, getConfig());
    }

    public static boolean isClassHandledByConfigProducer(Type requiredType) {
        // TODO Make an ArrayList and use Contains?
        return requiredType == String.class
                || requiredType == Boolean.class
                || requiredType == Boolean.TYPE
                || requiredType == Integer.class
                || requiredType == Integer.TYPE
                || requiredType == Long.class
                || requiredType == Long.TYPE
                || requiredType == Float.class
                || requiredType == Float.TYPE
                || requiredType == Double.class
                || requiredType == Double.TYPE
                || requiredType == Short.class
                || requiredType == Short.TYPE
                || requiredType == Byte.class
                || requiredType == Byte.TYPE
                || requiredType == Character.class
                || requiredType == Character.TYPE
                || requiredType == OptionalInt.class
                || requiredType == OptionalLong.class
                || requiredType == OptionalDouble.class
                || requiredType == ConfigValue.class;
    }
}
