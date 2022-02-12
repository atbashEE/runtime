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
package be.atbash.runtime.config.mp.util;

import be.atbash.runtime.config.mp.ConfigValueImpl;
import be.atbash.runtime.config.mp.inject.ConfigPropertyLiteral;
import be.atbash.runtime.config.mp.util.testclass.SomeClass;
import jakarta.enterprise.inject.spi.AnnotatedMember;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.InjectionPoint;
import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Supplier;

import static be.atbash.runtime.config.mp.converter.Converters.INTEGER_CONVERTER;
import static be.atbash.runtime.config.mp.converter.Converters.STRING_CONVERTER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigProducerUtilTest {

    @Mock
    private InjectionPoint injectionPointMock;

    @Mock
    private AnnotatedMember annotatedMemberMock;

    @Mock
    private AnnotatedType annotatedTypeMock;

    @Mock
    private Member memberMock;

    @Mock
    private Config configMock;

    // Used by rawTypeOf_genericArrayType and public to make it easier to find (.getField() is usable immediately to find a public)
    public List<String>[] genericArray;

    // Used by getValue methods
    public List<String> someList;

    // Used by getValue methods
    public Set<String> someSet;

    // Used by getValue methods
    public Optional<String> someOptional;

    // Used by getValue methods
    public Supplier<String> someSupplier;


    @Test
    void getConfigKey_fromConfigProperty() {

        String key = ConfigProducerUtil.getConfigKey(injectionPointMock, new ConfigPropertyLiteral("test"), true);
        assertThat(key).isEqualTo("test");
    }

    @Test
    void getConfigKey_withConfigPropertyHavingNoName() {
        when(injectionPointMock.getAnnotated()).thenReturn(annotatedMemberMock);
        when(annotatedMemberMock.getDeclaringType()).thenReturn(annotatedTypeMock);
        when(annotatedMemberMock.getJavaMember()).thenReturn(memberMock);
        when(annotatedTypeMock.getJavaClass()).thenReturn(SomeClass.class);
        when(memberMock.getName()).thenReturn("fieldName");

        String key = ConfigProducerUtil.getConfigKey(injectionPointMock, new ConfigPropertyLiteral(""), true);
        assertThat(key).isEqualTo("be.atbash.runtime.config.mp.util.testclass.SomeClass.fieldName");
    }

    @Test
    void getConfigKey_fromInjectionPoint() {

        when(injectionPointMock.getAnnotated()).thenReturn(annotatedMemberMock);
        when(annotatedMemberMock.getDeclaringType()).thenReturn(annotatedTypeMock);
        when(annotatedMemberMock.getJavaMember()).thenReturn(memberMock);
        when(annotatedTypeMock.getJavaClass()).thenReturn(SomeClass.class);
        when(memberMock.getName()).thenReturn("fieldName");

        String key = ConfigProducerUtil.getConfigKey(injectionPointMock, null, true);
        assertThat(key).isEqualTo("be.atbash.runtime.config.mp.util.testclass.SomeClass.fieldName");
    }

    @Test
    void getConfigKey_fromInjectionPoint_simpleName() {

        when(injectionPointMock.getAnnotated()).thenReturn(annotatedMemberMock);
        when(annotatedMemberMock.getJavaMember()).thenReturn(memberMock);
        when(memberMock.getName()).thenReturn("fieldName");

        String key = ConfigProducerUtil.getConfigKey(injectionPointMock, null, false);
        assertThat(key).isEqualTo("fieldName");
    }

    @Test
    void getValue() {
        ConfigValue configValue = new ConfigValueImpl.ConfigValueBuilder()
                .withName("foo")
                .withValue("1234")
                .build();
        when(configMock.getConfigValue("foo")).thenReturn(configValue);

        when(configMock.getConverter(Integer.class)).thenReturn(Optional.of(INTEGER_CONVERTER));

        Object foo = ConfigProducerUtil.getValue("foo", Integer.class, null, configMock);
        Assertions.assertThat(foo).isInstanceOf(Integer.class);
        Assertions.assertThat((Integer) foo).isEqualTo(1234);

    }

    @Test
    void getValue_useDefault() {
        ConfigValue configValue = new ConfigValueImpl.ConfigValueBuilder()
                .withName("foo")
                .withValue(null)
                .build();
        when(configMock.getConfigValue("foo")).thenReturn(configValue);

        when(configMock.getConverter(Integer.class)).thenReturn(Optional.of(INTEGER_CONVERTER));

        Object foo = ConfigProducerUtil.getValue("foo", Integer.class, "4321", configMock);
        Assertions.assertThat(foo).isInstanceOf(Integer.class);
        Assertions.assertThat((Integer) foo).isEqualTo(4321);

    }

    @Test
    void getValue_noDefault() {
        ConfigValue configValue = new ConfigValueImpl.ConfigValueBuilder()
                .withName("foo")
                .withValue(null)
                .build();
        when(configMock.getConfigValue("foo")).thenReturn(configValue);

        when(configMock.getConverter(Integer.class)).thenReturn(Optional.of(INTEGER_CONVERTER));

        NoSuchElementException exception = Assertions.catchThrowableOfType(() ->
                        ConfigProducerUtil.getValue("foo", Integer.class, null, configMock)
                , NoSuchElementException.class);

        Assertions.assertThat(exception.getMessage()).isEqualTo("MPCONFIG-114: The config property 'foo' is required but it could not be found in any config source");
    }

    @Test
    void getValue_withList() throws NoSuchFieldException {
        ConfigValue configValue = new ConfigValueImpl.ConfigValueBuilder()
                .withName("foo")
                .withValue("a,b,c")
                .build();
        when(configMock.getConfigValue("foo")).thenReturn(configValue);

        Type listType = this.getClass().getField("someList").getGenericType();
        when(configMock.getConverter(String.class)).thenReturn(Optional.of(STRING_CONVERTER));

        Object foo = ConfigProducerUtil.getValue("foo", listType, null, configMock);
        Assertions.assertThat(foo).isInstanceOf(List.class);
        List<String> data = (List<String>) foo;
        Assertions.assertThat(data).hasSize(3);
        Assertions.assertThat(data).contains("a", "b", "c");

    }


    @Test
    void getValue_withSet() throws NoSuchFieldException {
        ConfigValue configValue = new ConfigValueImpl.ConfigValueBuilder()
                .withName("foo")
                .withValue("a,b,c")
                .build();
        when(configMock.getConfigValue("foo")).thenReturn(configValue);

        Type setType = this.getClass().getField("someSet").getGenericType();
        when(configMock.getConverter(String.class)).thenReturn(Optional.of(STRING_CONVERTER));

        Object foo = ConfigProducerUtil.getValue("foo", setType, null, configMock);
        Assertions.assertThat(foo).isInstanceOf(Set.class);
        Set<String> data = (Set<String>) foo;
        Assertions.assertThat(data).hasSize(3);
        Assertions.assertThat(data).contains("a", "b", "c");

    }

    @Test
    void getValue_withOptional() throws NoSuchFieldException {
        ConfigValue configValue = new ConfigValueImpl.ConfigValueBuilder()
                .withName("foo")
                .withValue("a")
                .build();
        when(configMock.getConfigValue("foo")).thenReturn(configValue);

        Type optionalType = this.getClass().getField("someOptional").getGenericType();
        when(configMock.getConverter(String.class)).thenReturn(Optional.of(STRING_CONVERTER));

        Object foo = ConfigProducerUtil.getValue("foo", optionalType, null, configMock);
        Assertions.assertThat(foo).isInstanceOf(Optional.class);
        Optional<String> data = (Optional<String>) foo;
        Assertions.assertThat(data).hasValue("a");

    }

    @Test
    void getValue_withSupplier() throws NoSuchFieldException {
        ConfigValue configValue = new ConfigValueImpl.ConfigValueBuilder()
                .withName("foo")
                .withValue("a")
                .build();
        when(configMock.getConfigValue("foo")).thenReturn(configValue);

        Type supplierType = this.getClass().getField("someSupplier").getGenericType();
        when(configMock.getConverter(String.class)).thenReturn(Optional.of(STRING_CONVERTER));

        Object foo = ConfigProducerUtil.getValue("foo", supplierType, null, configMock);
        // Supplier is special and only useable for CDI injection. The CDI producers handle the creation of the Supplier
        //and the getValue is only called when the get(à of the Supplier is called.)
        Assertions.assertThat(foo).isInstanceOf(String.class);
        Assertions.assertThat((String) foo).isEqualTo("a");

    }


    @Test
    public void rawTypeOf_class() {
        Class<Object> aClass = ConfigProducerUtil.rawTypeOf(Integer.class);
        assertThat(aClass).isEqualTo(Integer.class);
    }

    @Test
    public void rawTypeOf_parameterisedType() {
        List<String> list = new ArrayList<>();
        assertThat(list.getClass().getGenericInterfaces()[0] instanceof ParameterizedType).isTrue();

        Class<Object> aClass = ConfigProducerUtil.rawTypeOf(list.getClass().getGenericInterfaces()[0]);
        assertThat(aClass).isEqualTo(List.class);
    }

    @Test
    public void rawTypeOf_parameterisedType_nested() {
        Set<List<String>> list = new HashSet<>();
        assertThat(list.getClass().getGenericInterfaces()[0] instanceof ParameterizedType).isTrue();

        Class<Object> aClass = ConfigProducerUtil.rawTypeOf(list.getClass().getGenericInterfaces()[0]);
        assertThat(aClass).isEqualTo(Set.class);
    }

    @Test
    public void rawTypeOf_genericArrayType() throws NoSuchFieldException {
        Type genericType = this.getClass().getField("genericArray").getGenericType();

        Class<Object> aClass = ConfigProducerUtil.rawTypeOf(genericType);
        assertThat(aClass.isArray()).isTrue();
        assertThat(aClass.getComponentType()).isEqualTo(List.class);
    }

    @Test
    public void getDefaultForType_boolean() {
        String defaultForType = ConfigProducerUtil.getDefaultForType(boolean.class);
        Assertions.assertThat(defaultForType).isEqualTo("false");
    }

    @Test
    public void getDefaultForType_char() {
        String defaultForType = ConfigProducerUtil.getDefaultForType(char.class);
        Assertions.assertThat(defaultForType).isNull();
    }

    @Test
    public void getDefaultForType_integer() {
        String defaultForType = ConfigProducerUtil.getDefaultForType(int.class);
        Assertions.assertThat(defaultForType).isEqualTo("0");
    }

    @Test
    public void getDefaultForType_float() {
        // As an example of something else.
        String defaultForType = ConfigProducerUtil.getDefaultForType(Float.class);
        Assertions.assertThat(defaultForType).isNull();
    }

    @Test
    public void getValue_ip() {
        Set<Annotation> annotations = Set.of(new ConfigPropertyLiteral("foo"));
        when(injectionPointMock.getQualifiers()).thenReturn(annotations);

        when(injectionPointMock.getType()).thenReturn(Integer.class);

        ConfigValue configValue = new ConfigValueImpl.ConfigValueBuilder()
                .withName("foo")
                .withValue("1234")
                .build();
        when(configMock.getConfigValue("foo")).thenReturn(configValue);
        when(configMock.getConverter(Integer.class)).thenReturn(Optional.of(INTEGER_CONVERTER));

        Object foo = ConfigProducerUtil.getValue(injectionPointMock, configMock);
        Assertions.assertThat(foo).isInstanceOf(Integer.class);
        Assertions.assertThat((Integer) foo).isEqualTo(1234);
    }

    @Test
    public void getValue_ip_defaultValue() {
        Set<Annotation> annotations = Set.of(new ConfigPropertyLiteral("foo", "4321"));
        when(injectionPointMock.getQualifiers()).thenReturn(annotations);

        when(injectionPointMock.getType()).thenReturn(Integer.class);

        ConfigValue configValue = new ConfigValueImpl.ConfigValueBuilder()
                .withName("foo")
                .withValue(null)
                .build();
        when(configMock.getConfigValue("foo")).thenReturn(configValue);
        when(configMock.getConverter(Integer.class)).thenReturn(Optional.of(INTEGER_CONVERTER));

        Object foo = ConfigProducerUtil.getValue(injectionPointMock, configMock);
        Assertions.assertThat(foo).isInstanceOf(Integer.class);
        Assertions.assertThat((Integer) foo).isEqualTo(4321);
    }


    @Test
    public void getValue_ip_noConfigProperty() {
        Object foo = ConfigProducerUtil.getValue(injectionPointMock, configMock);
        Assertions.assertThat(foo).isNull();  // Since there is no @ConfigProperty on InjectPoint, we cannot determine the proeprty key

    }

    @Test
    public void getConfigValue() {
        Set<Annotation> annotations = Set.of(new ConfigPropertyLiteral("foo"));
        when(injectionPointMock.getQualifiers()).thenReturn(annotations);

        ConfigValue configValue = new ConfigValueImpl.ConfigValueBuilder()
                .withName("foo")
                .withValue("1234")
                .withRawValue("1234")
                .build();
        when(configMock.getConfigValue("foo")).thenReturn(configValue);

        ConfigValue value = ConfigProducerUtil.getConfigValue(injectionPointMock, configMock);
        Assertions.assertThat(value).isNotNull();
        Assertions.assertThat(value.getName()).isEqualTo("foo");
        Assertions.assertThat(value.getValue()).isEqualTo("1234");
        Assertions.assertThat(value.getRawValue()).isEqualTo("1234");

        Assertions.assertThat(value == configValue).isTrue();
    }

    @Test
    public void getConfigValue_noConfigProperty() {
        ConfigValue value = ConfigProducerUtil.getConfigValue(injectionPointMock, configMock);
        Assertions.assertThat(value).isNull();
    }


    @Test
    public void getConfigValue_notFound() {
        Set<Annotation> annotations = Set.of(new ConfigPropertyLiteral("foo", "4321"));
        when(injectionPointMock.getQualifiers()).thenReturn(annotations);

        ConfigValue configValue = new ConfigValueImpl.ConfigValueBuilder()
                .withName("foo")
                .withRawValue(null)
                .build();
        when(configMock.getConfigValue("foo")).thenReturn(configValue);

        ConfigValue value = ConfigProducerUtil.getConfigValue(injectionPointMock, configMock);
        Assertions.assertThat(value).isNotNull();
        Assertions.assertThat(value.getName()).isEqualTo("foo");
        Assertions.assertThat(value.getValue()).isEqualTo("4321");
        Assertions.assertThat(value.getRawValue()).isNull();

        Assertions.assertThat(value == configValue).isFalse();
    }
}