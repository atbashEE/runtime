/*
 * Copyright 2021-2023 Rudy De Busscher (https://www.atbash.be)
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
package be.atbash.runtime.config.mp;

import be.atbash.runtime.config.mp.inject.ConfigInjectionBean;
import be.atbash.runtime.config.mp.inject.ConfigProducer;
import be.atbash.runtime.config.mp.prefix.ConfigPropertiesProducer;
import be.atbash.runtime.config.mp.prefix.TypesBeanAttributes;
import be.atbash.runtime.config.mp.util.AnnotationUtil;
import be.atbash.runtime.config.mp.util.ConfigProducerUtil;
import be.atbash.runtime.core.data.deployment.AbstractDeployment;
import be.atbash.runtime.core.data.deployment.CurrentDeployment;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.*;
import jakarta.inject.Provider;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * CDI Extension to handle MP Config specification.
 * <p>
 * Based on code by Jeff Mesnil (Red Hat)
 */
public class ConfigExtension implements Extension {

    private static final Logger LOGGER = Logger.getLogger(ConfigExtension.class.getName());

    // All InjectionPoints with @ConfigProperty
    private final Set<InjectionPoint> configPropertyInjectionPoints = new HashSet<>();

    // All classes with @ConfigProperties
    private final Set<AnnotatedType<?>> configProperties = new HashSet<>();

    // All InjectionPoints with @ConfigProperties
    private final Set<InjectionPoint> configPropertiesInjectionPoints = new HashSet<>();

    // All Classes (Types so that we can use if for CDI producer Bean) of the InjectionPoints with @ConfigProperties
    private final Set<Type> configPropertiesBeanTypes = new HashSet<>();

    protected void beforeBeanDiscovery(@Observes BeforeBeanDiscovery event, BeanManager bm) {
        AbstractDeployment deployment = CurrentDeployment.getInstance().getCurrent();
        boolean configDisabled = !getBooleanFromData(deployment, MPConfigModuleConstant.MPCONFIG_ENABLED);

        if (configDisabled) {
            return;
        }

        boolean originalJarPackaging = getClass().getClassLoader().getResource("META-INF/OriginalJarPackaging") != null;

        if (originalJarPackaging) {
            // NO beans.xml in the mp-config; so register manually.
            // If using Shade plugin or Runtime maven plugin, the uber jar contains a beans.xml
            // and thus this manual registration should not be done as otherwise we have 2 beans for same Injection point
            // and an Exception
            AnnotatedType<ConfigProducer> configBean = bm.createAnnotatedType(ConfigProducer.class);
            event.addAnnotatedType(configBean, ConfigProducer.class.getName());
        }
    }

    protected <T> void storeConfigPropertiesType(@Observes @WithAnnotations(ConfigProperties.class) ProcessAnnotatedType<T> event) {
        // We keep this processing so that we can warn that MPConfig is disabled but annotations are found. (FIXME to implement the warning)
        AnnotatedType<?> type = event.getAnnotatedType();

        if (type.getJavaClass().isAnnotationPresent(ConfigProperties.class)) {
            configProperties.add(type);
            event.veto();  // We have ConfigPropertiesProducer for this.
            return;
        }

        for (AnnotatedField<?> field : type.getFields()) {
            Class<?> memberClass = field.getJavaMember().getType();
            if (memberClass.isAnnotationPresent(ConfigProperties.class)) {
                configPropertiesBeanTypes.add(memberClass);
            }
        }
    }

    protected void processConfigInjectionPoints(@Observes ProcessInjectionPoint<?, ?> pip) {
        // We keep this processing so that we can warn that MPConfig is disabled but annotations are found. (FIXME to implement the warning)
        InjectionPoint injectionPoint = pip.getInjectionPoint();
        if (injectionPoint.getAnnotated().isAnnotationPresent(ConfigProperty.class)) {
            configPropertyInjectionPoints.add(injectionPoint);
        }

        if (injectionPoint.getAnnotated().isAnnotationPresent(ConfigProperties.class)) {
            Type type = injectionPoint.getType();
            if (validTypeForConfigProperties(type, injectionPoint)) {
                configPropertiesInjectionPoints.add(injectionPoint);
            }
            configPropertiesInjectionPoints.add(injectionPoint);
        }

    }

    private boolean validTypeForConfigProperties(Type type, InjectionPoint injectionPoint) {
        AbstractDeployment deployment = CurrentDeployment.getInstance().getCurrent();
        boolean configDisabled = !getBooleanFromData(deployment, MPConfigModuleConstant.MPCONFIG_ENABLED);

        if (configDisabled) {
            // Don't do the checks when validation is disabled.
            return true;
        }

        // No need to record these validations errors since the default CDI validation will already create DeploymentException
        // and our extension doesn't get the chance to add them.
        boolean result = true;
        if (!(type instanceof Class)) {
            String msg = String.format("MPCONFIG-012: Injection point with @ConfigProperties is only supported with a class but found Type '%s' at %s", type, formatInjectionPoint(injectionPoint));
            LOGGER.severe(msg);
            result = false;
        } else {
            Class<?> aClass = (Class<?>) type;
            if (aClass.isPrimitive() || aClass.isArray()) {
                String msg = String.format("MPCONFIG-012: Injection point with @ConfigProperties is not supported with a primitive or array and found Type '%s' at %s", type, formatInjectionPoint(injectionPoint));
                LOGGER.severe(msg);
                result = false;
            }
        }
        return result;

    }

    protected void registerCustomBeans(@Observes AfterBeanDiscovery event, BeanManager bm) {
        AbstractDeployment deployment = CurrentDeployment.getInstance().getCurrent();
        boolean configDisabled = !getBooleanFromData(deployment, MPConfigModuleConstant.MPCONFIG_ENABLED);

        if (configDisabled) {
            // Don't perform this since MPConfig is disabled.
            return;
        }

        Set<Class<?>> customTypes = new HashSet<>();
        for (InjectionPoint ip : configPropertyInjectionPoints) {
            Type requiredType = ip.getType();
            if (requiredType instanceof Class && !ConfigProducer.isClassHandledByConfigProducer(requiredType)) {
                // type is not produced by ConfigProducer
                customTypes.add((Class<?>) requiredType);
            }
        }

        // If not produced by ConfigProducer, ConfigInjectionBean will handle it.
        customTypes.stream().map(customType -> new ConfigInjectionBean<>(bm, customType)).forEach(event::addBean);

        // Handle the @ConfigProperties injections
        if (!configPropertiesBeanTypes.isEmpty()) {
            registerConfigPropertiesBean(event, bm);
        }
    }

    private void registerConfigPropertiesBean(AfterBeanDiscovery event, BeanManager bm) {
        AnnotatedType<ConfigPropertiesProducer> objectProducerType = bm.createAnnotatedType(ConfigPropertiesProducer.class);

        // create a synthetic bean based on the ConfigPropertyProducer which
        // has a method we can use to create the correct objects based on
        // the InjectionPoint
        BeanAttributes<?> objectBeanAttributes = null;

        // first find the producer method
        AnnotatedMethod<? super ConfigPropertiesProducer> objectProducerMethod = null;

        for (AnnotatedMethod<? super ConfigPropertiesProducer> m : objectProducerType.getMethods()) {
            String methodName = m.getJavaMember().getName();
            if (methodName.equals("getGenericObject")) {
                objectBeanAttributes = bm.createBeanAttributes(m);
                objectProducerMethod = m;
                break;
            }
        }

        Bean<?> bean = bm.createBean(new TypesBeanAttributes<>(objectBeanAttributes) {
                                         @Override
                                         public Set<Type> getTypes() {
                                             return configPropertiesBeanTypes;
                                         }
                                     }, ConfigPropertiesProducer.class,
                bm.getProducerFactory(objectProducerMethod, null));
        event.addBean(bean);
    }

    protected void validate(@Observes AfterDeploymentValidation event) {
        AbstractDeployment deployment = CurrentDeployment.getInstance().getCurrent();
        boolean validationDisabled = getBooleanFromData(deployment, MPConfigModuleConstant.MPCONFIG_VALIDATION_DISABLED);
        boolean configDisabled = !getBooleanFromData(deployment, MPConfigModuleConstant.MPCONFIG_ENABLED);
        if (validationDisabled || configDisabled) {
            // We don't want validation to happen or MPConfig is not enabled at all (sniffer or config based).
            return;
        }
        // Thread.currentThread().getContextClassLoader()  == Jetty WebAppClassLoader
        Config config = ConfigProvider.getConfig(Thread.currentThread().getContextClassLoader());
        validateConfigPropertyInjectionPoints(event, config);
        validateConfigProperties(event, config);

    }

    private boolean getBooleanFromData(AbstractDeployment deployment, String key) {
        return Boolean.parseBoolean(deployment.getDeploymentData(key));
    }

    private void validateConfigProperties(AfterDeploymentValidation adv, Config config) {


        Set<Type> allTypes = new HashSet<>();
        allTypes.addAll(configPropertiesBeanTypes);
        allTypes.addAll(configProperties.stream().map(AnnotatedType::getJavaClass).collect(Collectors.toList()));
        // With TCK, we have a class with @ConfigProperties that isn't used. But it is checked.
        // That is the reason for the above statement

        for (Type configPropertiesBeanType : allTypes) {
            // For eachClass (Type) that has @ConfigProperties on it, check to see if there are the configuration values
            // found in the system.


            // We only classes in the allTypes set, so safe to do.
            Class<?> configPropertiesBeanClass = (Class<?>) configPropertiesBeanType;

            List<InjectionPoint> injectionsPointsOfClass = findAllInjectionsPointsOfClass(configPropertiesBeanClass);
            List<FieldConfigData> fieldsData = Arrays.stream(configPropertiesBeanClass.getFields())
                    .map(FieldConfigData::new).collect(Collectors.toList());

            Optional<String> classPrefix = getClassPrefix(configPropertiesBeanClass);

            if (injectionsPointsOfClass.isEmpty()) {
                // This is only useful for TCK
                validateBasedOnClass(adv, config, configPropertiesBeanType, fieldsData, classPrefix);
            } else {
                validateBasedOnInjectionPoints(adv, config, configPropertiesBeanType, injectionsPointsOfClass, fieldsData, classPrefix);
            }
        }
    }

    private void validateBasedOnClass(AfterDeploymentValidation adv, Config config, Type configPropertiesBeanType, List<FieldConfigData> fieldsData, Optional<String> classPrefix) {

        String prefix = classPrefix.orElse("");
        for (FieldConfigData configData : fieldsData) {

            try {
                String defaultValue = getDefaultValue(configData.getConfigProperty(), ConfigProducerUtil.rawTypeOf(configData.getType()));

                // Check if the value can be injected. This may cause duplicated config reads (to validate and to inject).
                ConfigProducerUtil.getValue(prefix + configData.getConfigKeyName(), configData.getType(), defaultValue, config);
            } catch (Exception e) {
                String msg = String.format("MPCONFIG-011: Failed to Inject for key %s into %s %s", prefix + configData.getConfigKeyName(), configPropertiesBeanType,
                        e.getLocalizedMessage());

                adv.addDeploymentProblem(new DefinitionException(msg, e));
            }
        }

    }

    private void validateBasedOnInjectionPoints(AfterDeploymentValidation adv, Config config, Type configPropertiesBeanType, List<InjectionPoint> injectionsPointsOfClass, List<FieldConfigData> fieldsData, Optional<String> classPrefix) {
        for (InjectionPoint injectionPoint : injectionsPointsOfClass) {
            ConfigProperties configProperties = AnnotationUtil.getConfigPropertiesAnnotation(injectionPoint);
            // should never be null !!
            Optional<String> injectionPrefix = AnnotationUtil.parsePrefix(configProperties);
            String prefix = determinePrefix(injectionPrefix, classPrefix);
            for (FieldConfigData configData : fieldsData) {

                try {
                    String defaultValue = getDefaultValue(configData.getConfigProperty(), ConfigProducerUtil.rawTypeOf(configData.getType()));

                    // Check if the value can be injected. This may cause duplicated config reads (to validate and to inject).
                    ConfigProducerUtil.getValue(prefix + configData.getConfigKeyName(), configData.getType(), defaultValue, config);
                } catch (Exception e) {
                    String msg = String.format("MPCONFIG-011 Failed to Inject for key %s into %s %s", prefix + configData.getConfigKeyName(), configPropertiesBeanType,
                            e.getLocalizedMessage());

                    adv.addDeploymentProblem(new DefinitionException(msg, e));
                }
            }
        }
    }

    /**
     * Determine the prefix that needs to be used.  Injection has precedence over the prefix defined at the class level.
     * If no prefix defined at all, returns empty string
     *
     * @param injectionPrefix Prefix config on InjectionPoint
     * @param classPrefix     Prefix config on Class.
     * @return Prefix to use based on config on InjectionPoint or CLass.
     */
    private String determinePrefix(Optional<String> injectionPrefix, Optional<String> classPrefix) {
        return injectionPrefix.orElse(classPrefix.orElse(""));
    }

    private Optional<String> getClassPrefix(Class<?> configPropertiesBeanType) {
        Optional<String> result;
        ConfigProperties configProperties = configPropertiesBeanType.getAnnotation(ConfigProperties.class);
        String prefix = configProperties.prefix();
        if (ConfigProperties.UNCONFIGURED_PREFIX.equals(prefix)) {
            result = Optional.empty();
        } else {
            result = Optional.of(prefix + ".");
        }
        return result;

    }

    private List<InjectionPoint> findAllInjectionsPointsOfClass(Class<?> javaClass) {
        return configPropertiesInjectionPoints.stream()
                .filter(ip -> javaClass.isAssignableFrom((Class<?>) ip.getType()))
                .collect(Collectors.toList());
    }

    private String getDefaultValue(ConfigProperty configPropertyField, Class<Object> type) {
        if (configPropertyField != null && !ConfigProperty.UNCONFIGURED_VALUE.equals(configPropertyField.defaultValue())) {
            return configPropertyField.defaultValue();
        }
        return ConfigProducerUtil.getDefaultForType(type);
    }

    private void validateConfigPropertyInjectionPoints(AfterDeploymentValidation event, Config config) {
        for (InjectionPoint injectionPoint : getConfigPropertyInjectionPoints()) {
            Type type = injectionPoint.getType();

            // We don't validate the Optional / Provider / Supplier / ConfigValue for defaultValue.
            if (type instanceof Class && org.eclipse.microprofile.config.ConfigValue.class.isAssignableFrom((Class<?>) type)
                    || type instanceof Class && OptionalInt.class.isAssignableFrom((Class<?>) type)
                    || type instanceof Class && OptionalLong.class.isAssignableFrom((Class<?>) type)
                    || type instanceof Class && OptionalDouble.class.isAssignableFrom((Class<?>) type)
                    || type instanceof ParameterizedType
                    && (Optional.class.isAssignableFrom((Class<?>) ((ParameterizedType) type).getRawType())
                    || Provider.class.isAssignableFrom((Class<?>) ((ParameterizedType) type).getRawType())
                    || Supplier.class.isAssignableFrom((Class<?>) ((ParameterizedType) type).getRawType()))) {
                continue;
            }

            ConfigProperty configProperty = injectionPoint.getAnnotated().getAnnotation(ConfigProperty.class);
            String name;
            try {
                name = ConfigProducerUtil.getConfigKey(injectionPoint, configProperty, true);
            } catch (IllegalStateException e) {
                String msg = String.format("MPCONFIG-011: Failed to Inject @ConfigProperty for key %s into %s %s", null, formatInjectionPoint(injectionPoint),
                        e.getLocalizedMessage());

                event.addDeploymentProblem(new DefinitionException(msg, e));
                continue;
            }


            try {
                // Check if the value can be 'injected'/resolved.
                ConfigProducerUtil.getValue(injectionPoint, config);
            } catch (Exception e) {
                String msg = String.format("MPCONFIG-011 Failed to Inject @ConfigProperty for key %s into %s %s", name, formatInjectionPoint(injectionPoint),
                        e.getLocalizedMessage());

                event.addDeploymentProblem(new DefinitionException(msg, e));
            }
        }
    }

    protected Set<InjectionPoint> getConfigPropertyInjectionPoints() {
        return configPropertyInjectionPoints;
    }

    /**
     * Formats InjectPoint information for Exception messages.<br>
     * <br>
     * <p>
     * 3 possible InjectionPoint types are considered:<br>
     * <br>
     *
     * <b>Fields</b><br>
     * Given: java.lang.String
     * io.smallrye.config.inject.ValidateInjectionTest$SkipPropertiesTest$SkipPropertiesBean.missingProp<br>
     * Returns: io.smallrye.config.inject.ValidateInjectionTest$SkipPropertiesTest$SkipPropertiesBean.missingProp<br>
     * <br>
     *
     * <b>Method parameters</b><br>
     * Given: private void
     * io.smallrye.config.inject.ValidateInjectionTest$MethodUnnamedPropertyTest$MethodUnnamedPropertyBean.methodUnnamedProperty(java.lang.String)<br>
     * Returns:
     * io.smallrye.config.inject.ValidateInjectionTest$MethodUnnamedPropertyTest$MethodUnnamedPropertyBean.methodUnnamedProperty(String)<br>
     * <br>
     *
     * <b>Constructor parameters</b><br>
     * Given: public
     * io.smallrye.config.inject.ValidateInjectionTest$ConstructorUnnamedPropertyTest$ConstructorUnnamedPropertyBean(java.lang.String)<br>
     * Returns:
     * io.smallrye.config.inject.ValidateInjectionTest$ConstructorUnnamedPropertyTest$ConstructorUnnamedPropertyBean(String)
     */
    private static String formatInjectionPoint(InjectionPoint injectionPoint) {

        Member member = injectionPoint.getMember();

        StringBuilder sb = new StringBuilder();
        sb.append(member.getDeclaringClass().getName());

        if (member instanceof Field) {
            sb.append(".").append(member.getName());
        } else if (member instanceof Method) {
            sb.append(".").append(member.getName());
            appendParameterTypes(sb, (Method) member);
        } else if (member instanceof Constructor) {
            appendParameterTypes(sb, (Constructor<?>) member);
        }
        return sb.toString();
    }

    private static void appendParameterTypes(StringBuilder sb, Executable executable) {
        sb.append("(").append(Arrays.stream(executable.getParameterTypes()).map(Class::getSimpleName).collect(Collectors.joining(", "))).append(")");
    }

    /**
     * Data about Field in {@code @ConfigProperties} annotaed classes.
     */
    private static class FieldConfigData {

        private final String configKeyName;
        private final Type type;
        private final ConfigProperty configProperty; // can be null.

        public FieldConfigData(Field field) {
            configKeyName = getPropertyName(field);
            type = field.getGenericType();
            configProperty = field.getAnnotation(ConfigProperty.class);
        }

        private String getPropertyName(Field field) {
            ConfigProperty configPropertyField = field.getAnnotation(ConfigProperty.class);

            String propertyName;
            if (configPropertyField != null && !configPropertyField.name().isEmpty()) {
                propertyName = configPropertyField.name();
            } else {
                propertyName = field.getName();
            }
            return propertyName;
        }

        public String getConfigKeyName() {
            return configKeyName;
        }

        public Type getType() {
            return type;
        }

        public ConfigProperty getConfigProperty() {
            return configProperty;
        }
    }
}
