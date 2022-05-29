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
package be.atbash.runtime.security.jwt.jaxrs;

import be.atbash.runtime.security.jwt.jaxrs.testclasses.Class1;
import be.atbash.runtime.security.jwt.jaxrs.testclasses.Class2;
import be.atbash.util.TestReflectionUtils;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class JWTAuthorizationFilterRegistrarTest {

    @Mock
    ResourceInfo resourceInfoMock;

    @Mock
    private FeatureContext contextMock;

    @Captor
    private ArgumentCaptor<ContainerRequestFilter> containerRequestFilterCaptor;

    @Test
    void configure() throws NoSuchMethodException, NoSuchFieldException {

        Method method1 = Class1.class.getMethod("method1");
        Mockito.when(resourceInfoMock.getResourceMethod()).thenReturn(method1);

        JWTAuthorizationFilterRegistrar filterRegistrar = new JWTAuthorizationFilterRegistrar();

        filterRegistrar.configure(resourceInfoMock, contextMock);

        Mockito.verify(contextMock).register(containerRequestFilterCaptor.capture());
        Assertions.assertThat(containerRequestFilterCaptor.getValue()).isInstanceOf(RolesAllowedFilter.class);

        RolesAllowedFilter filter = (RolesAllowedFilter) containerRequestFilterCaptor.getValue();
        List<String> allowedRoles = TestReflectionUtils.getValueOf(filter, "allowedRoles");
        Assertions.assertThat(allowedRoles).containsExactly("role1");
    }

    @Test
    void configure_permitAll() throws NoSuchMethodException, NoSuchFieldException {

        Method method1 = Class1.class.getMethod("method2");
        Mockito.when(resourceInfoMock.getResourceMethod()).thenReturn(method1);

        JWTAuthorizationFilterRegistrar filterRegistrar = new JWTAuthorizationFilterRegistrar();

        filterRegistrar.configure(resourceInfoMock, contextMock);

        Mockito.verify(contextMock, Mockito.never()).register(containerRequestFilterCaptor.capture());

    }

    @Test
    void configure_noAnnotation() throws NoSuchMethodException, NoSuchFieldException {

        Method method1 = Class1.class.getMethod("method3");
        Mockito.when(resourceInfoMock.getResourceMethod()).thenReturn(method1);
        Class class2 = Class1.class;
        Mockito.when(resourceInfoMock.getResourceClass()).thenReturn(class2);

        JWTAuthorizationFilterRegistrar filterRegistrar = new JWTAuthorizationFilterRegistrar();

        filterRegistrar.configure(resourceInfoMock, contextMock);

        Mockito.verify(contextMock).register(containerRequestFilterCaptor.capture());
        Assertions.assertThat(containerRequestFilterCaptor.getValue()).isInstanceOf(DenyAllFilter.class);
    }

    @Test
    void configure_explicitDeny() throws NoSuchMethodException, NoSuchFieldException {

        Method method1 = Class1.class.getMethod("method4");
        Mockito.when(resourceInfoMock.getResourceMethod()).thenReturn(method1);

        JWTAuthorizationFilterRegistrar filterRegistrar = new JWTAuthorizationFilterRegistrar();

        filterRegistrar.configure(resourceInfoMock, contextMock);

        Mockito.verify(contextMock).register(containerRequestFilterCaptor.capture());
        Assertions.assertThat(containerRequestFilterCaptor.getValue()).isInstanceOf(DenyAllFilter.class);
    }

    @Test
    void configure_multiple() throws NoSuchMethodException, NoSuchFieldException {

        Method method1 = Class1.class.getMethod("method5");
        Mockito.when(resourceInfoMock.getResourceMethod()).thenReturn(method1);

        JWTAuthorizationFilterRegistrar filterRegistrar = new JWTAuthorizationFilterRegistrar();

        Assertions.assertThatThrownBy(() -> filterRegistrar.configure(resourceInfoMock, contextMock)
                ).isInstanceOf(RuntimeException.class)
                .hasMessage("Multiple Permission annotations found at public void be.atbash.runtime.security.jwt.jaxrs.testclasses.Class1.method5()");
    }

    @Test
    void configure_classLevel() throws NoSuchMethodException, NoSuchFieldException {

        Method method1 = Class2.class.getMethod("method1");
        Mockito.when(resourceInfoMock.getResourceMethod()).thenReturn(method1);
        Class class2 = Class2.class;
        Mockito.when(resourceInfoMock.getResourceClass()).thenReturn(class2);

        JWTAuthorizationFilterRegistrar filterRegistrar = new JWTAuthorizationFilterRegistrar();

        filterRegistrar.configure(resourceInfoMock, contextMock);

        Mockito.verify(contextMock).register(containerRequestFilterCaptor.capture());
        Assertions.assertThat(containerRequestFilterCaptor.getValue()).isInstanceOf(RolesAllowedFilter.class);

        RolesAllowedFilter filter = (RolesAllowedFilter) containerRequestFilterCaptor.getValue();
        List<String> allowedRoles = TestReflectionUtils.getValueOf(filter, "allowedRoles");
        Assertions.assertThat(allowedRoles).containsExactly("role2");
    }


}