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
package be.atbash.runtime.config.mp.expression;

import be.atbash.runtime.config.mp.ConfigValueImpl;
import be.atbash.runtime.config.mp.sources.interceptor.ConfigSourceInterceptorContext;
import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpressionsTest {

    @Mock
    private ConfigSourceInterceptorContext contextMock;

    @Test
    void testCompile_pattern1() {
        when(contextMock.proceed("foo.bar")).thenReturn(new ConfigValueImpl.ConfigValueBuilder().withName("foo.bar").withRawValue("Atbash").build());

        CompositeNode expression = Expressions.parseExpression("${foo.bar}");
        String result = expression.evaluate(contextMock);
        Assertions.assertThat(result).isEqualTo("Atbash");
    }

    @Test
    void testCompile_pattern2() {
        CompositeNode expression = Expressions.parseExpression("This is a constant");
        Assertions.assertThat(expression.evaluate(null)).isEqualTo("This is a constant");
    }

    @Test
    void testCompile_pattern3() {
        when(contextMock.proceed("server.host")).thenReturn(new ConfigValueImpl.ConfigValueBuilder().withName("server.host").withRawValue("example.org").build());

        CompositeNode expression = Expressions.parseExpression("http://${server.host}/endpoint");
        String result = expression.evaluate(contextMock);
        Assertions.assertThat(result).isEqualTo("http://example.org/endpoint");

    }


    @Test
    void testCompile_pattern4() {
        when(contextMock.proceed("compose")).thenReturn(new ConfigValueImpl.ConfigValueBuilder().withName("compose").withRawValue("my.prop").build());
        when(contextMock.proceed("my.prop")).thenReturn(new ConfigValueImpl.ConfigValueBuilder().withName("my.prop").withRawValue("1234").build());

        CompositeNode expression = Expressions.parseExpression("${${compose}}");
        String result = expression.evaluate(contextMock);
        Assertions.assertThat(result).isEqualTo("1234");

    }

    @Test
    void testCompile_pattern5() {

        CompositeNode expression = Expressions.parseExpression("${my.prop:1234}");
        String result = expression.evaluate(contextMock);
        Assertions.assertThat(result).isEqualTo("1234");

    }

    @Test
    void testCompile_pattern6() {
        when(contextMock.proceed("mouse")).thenReturn(new ConfigValueImpl.ConfigValueBuilder().withName("mouse").withRawValue("mouse").build());

        CompositeNode expression = Expressions.parseExpression("cat,dog,${mouse},sea\\,turtle");
        String result = expression.evaluate(contextMock);
        Assertions.assertThat(result).isEqualTo("cat,dog,mouse,sea\\,turtle");

    }

    @Test
    void testCompile_pattern7() {
        when(contextMock.proceed("compose")).thenReturn(new ConfigValueImpl.ConfigValueBuilder().withName("compose").withRawValue("1234").build());
        when(contextMock.proceed("my.prop")).thenReturn(null);

        CompositeNode expression = Expressions.parseExpression("${my.prop:${compose}}");
        String result = expression.evaluate(contextMock);
        Assertions.assertThat(result).isEqualTo("1234");

    }

    @Test
    void testCompile_pattern8() {
        when(contextMock.proceed("compose")).thenReturn(new ConfigValueImpl.ConfigValueBuilder().withName("compose").withRawValue("4321").build());
        when(contextMock.proceed("my.prop")).thenReturn(null);


        CompositeNode expression = Expressions.parseExpression("${my.prop:65${compose}}");
        String result = expression.evaluate(contextMock);
        Assertions.assertThat(result).isEqualTo("654321");

    }

    @Test
    void testCompile_pattern9() {
        when(contextMock.proceed("my.prop")).thenReturn(new ConfigValueImpl.ConfigValueBuilder().withName("my.prop").withRawValue("1234").build());

        CompositeNode expression = Expressions.parseExpression("${my.prop:${compose:}}");
        String result = expression.evaluate(contextMock);
        Assertions.assertThat(result).isEqualTo("1234");

    }

    @Test
    void testCompile_pattern10() {

        CompositeNode expression = Expressions.parseExpression("\\${my.prop}");
        String result = expression.evaluate(contextMock);
        Assertions.assertThat(result).isEqualTo("\\${my.prop}");  // The unescape is not performed at this level.

    }

    @Test
    void testCompile_pattern11() {

        CompositeNode expression = Expressions.parseExpression("${expression}");
        String result = expression.evaluate(contextMock);
        Assertions.assertThat(result).isEqualTo(ConfigProperty.UNCONFIGURED_VALUE);


    }

    @Test
    void testCompile_pattern12() {

        when(contextMock.proceed("my.prop.three")).thenReturn(new ConfigValueImpl.ConfigValueBuilder().withName("my.prop.three").withRawValue("${my.prop.two}").build());
        //when(contextMock.proceed("my.prop.two")).thenReturn(new ConfigValueImpl.ConfigValueBuilder().withRawValue("${my.prop}").build());
        //when(contextMock.proceed("my.prop")).thenReturn(new ConfigValueImpl.ConfigValueBuilder().withRawValue("1234").build());

        CompositeNode expression = Expressions.parseExpression("${my.prop.three}");
        String result = expression.evaluate(contextMock);
        Assertions.assertThat(result).isEqualTo("${my.prop.two}");
        // We don't have the loop to resolve until we have a constant no here but in ExpressionConfigSourceInterceptor
    }

    @Test
    void testCompile_pattern13() {

        CompositeNode expression = Expressions.parseExpression("${expression}partial");
        String result = expression.evaluate(contextMock);
        Assertions.assertThat(result).isEqualTo(ConfigProperty.UNCONFIGURED_VALUE + "partial");
    }


}