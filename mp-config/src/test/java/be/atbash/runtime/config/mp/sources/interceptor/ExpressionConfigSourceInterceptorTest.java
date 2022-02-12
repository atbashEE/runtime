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

import be.atbash.runtime.config.mp.ConfigValueImpl;
import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.ConfigValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;

import static org.mockito.Mockito.lenient;


@ExtendWith(MockitoExtension.class)
class ExpressionConfigSourceInterceptorTest {

    @Mock
    private ConfigSourceInterceptorContext contextMock;

    @Test
    void getValue() {
        prepareMock("foo", "bar");
        ExpressionConfigSourceInterceptor interceptor = new ExpressionConfigSourceInterceptor(contextMock);
        ConfigValue configValue = interceptor.getValue(contextMock, "foo");

        Assertions.assertThat(configValue).isNotNull();
        Assertions.assertThat(configValue.getName()).isEqualTo("foo");
        Assertions.assertThat(configValue.getValue()).isEqualTo("bar");
    }

    @Test
    void getValue_expression() {
        prepareMock("foo", "${expr}");
        prepareMock("expr", "Atbash");
        ExpressionConfigSourceInterceptor interceptor = new ExpressionConfigSourceInterceptor(contextMock);
        ConfigValue configValue = interceptor.getValue(contextMock, "foo");

        Assertions.assertThat(configValue).isNotNull();
        Assertions.assertThat(configValue.getName()).isEqualTo("foo");
        Assertions.assertThat(configValue.getValue()).isEqualTo("Atbash");
    }

    @Test
    void getValue_expression_missing() {
        prepareMock("foo", "${expr}");
        ExpressionConfigSourceInterceptor interceptor = new ExpressionConfigSourceInterceptor(contextMock);

        NoSuchElementException exception = Assertions.catchThrowableOfType(() ->
                        interceptor.getValue(contextMock, "foo")
                , NoSuchElementException.class);

        Assertions.assertThat(exception.getMessage()).isEqualTo("MPCONFIG-015: Cannot resolve expression '${expr}'");

    }

    @Test
    void getValue_expression_notEnabled() {
        prepareMock("foo", "${expr}");
        prepareMock("mp.config.property.expressions.enabled", "false");

        ExpressionConfigSourceInterceptor interceptor = new ExpressionConfigSourceInterceptor(contextMock);

        ConfigValue configValue = interceptor.getValue(contextMock, "foo");

        Assertions.assertThat(configValue).isNotNull();
        Assertions.assertThat(configValue.getName()).isEqualTo("foo");
        Assertions.assertThat(configValue.getValue()).isEqualTo("${expr}");

    }

    @Test
    void getValue_expressionCaptureLoop() {
        prepareMock("foo", "${expr}");
        prepareMock("expr", "${expr}");
        ExpressionConfigSourceInterceptor interceptor = new ExpressionConfigSourceInterceptor(contextMock);

        IllegalArgumentException exception = Assertions.catchThrowableOfType(() ->
                        interceptor.getValue(contextMock, "foo")
                , IllegalArgumentException.class);

        Assertions.assertThat(exception.getMessage()).isEqualTo("MPCONFIG-035: Recursive expression expansion is too deep for 'foo'");

    }

    @Test
    void getValue_expression_escaped() {
        prepareMock("foo", "\\${expr}");

        ExpressionConfigSourceInterceptor interceptor = new ExpressionConfigSourceInterceptor(contextMock);

        ConfigValue configValue = interceptor.getValue(contextMock, "foo");

        Assertions.assertThat(configValue).isNotNull();
        Assertions.assertThat(configValue.getName()).isEqualTo("foo");
        Assertions.assertThat(configValue.getValue()).isEqualTo("${expr}");


    }

    private void prepareMock(String key, String value) {
        lenient().when(contextMock.proceed(key)).thenReturn(new ConfigValueImpl.ConfigValueBuilder().withName(key).withValue(value).withRawValue(value).build());
    }
}