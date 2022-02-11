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
import be.atbash.runtime.config.mp.expression.CompositeNode;
import be.atbash.runtime.config.mp.expression.Expressions;
import be.atbash.runtime.config.mp.expression.LiteralNode;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigValue;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.eclipse.microprofile.config.inject.ConfigProperty.UNCONFIGURED_VALUE;


/**
 * Interceptor that handles when configuration values contain expressions.
 * <p/>
 * Based on code by SmallRye Config.
 */
public class ExpressionConfigSourceInterceptor implements ConfigSourceInterceptor {

    private static final int MAX_DEPTH = 32;

    private final boolean enabled;

    public ExpressionConfigSourceInterceptor(ConfigSourceInterceptorContext context) {
        this.enabled = Optional.ofNullable(context.proceed(Config.PROPERTY_EXPRESSIONS_ENABLED))
                .map(ConfigValue::getValue)
                .map(Boolean::valueOf)
                .orElse(Boolean.TRUE);
    }

    @Override
    public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {

        ConfigValue configValue = context.proceed(name);

        if (!enabled) {
            return configValue;
        }

        if (configValue == null) {
            return null;
        }

        String expanded = configValue.getValue();  // expanded value is the one ConfigSource gave us art the moment
        // Let see if we need to process that further (like resolving expressions)

        int depth = 1;
        if (expanded.contains("$")) {
            //Having a $ might indicate we need to evaluate the expression. Let the parser decide if it is a real expression or a literal.
            CompositeNode compositeNode = Expressions.parseExpression(expanded);
            while (depth < MAX_DEPTH && !compositeNode.isNotAnExpression()) {

                expanded = compositeNode.evaluate(context);
                compositeNode = Expressions.parseExpression(expanded);
                depth++;
            }
        }

        if (depth == MAX_DEPTH) {
            throw new IllegalArgumentException(String.format("MPCONFIG-035: Recursive expression expansion is too deep for '%s'", name));
        }

        if (expanded.contains(UNCONFIGURED_VALUE)) {
            // Contains and not equals as we can have an expression and literal
            throw new NoSuchElementException(String.format("MPCONFIG-015: Cannot resolve expression '%s'", configValue.getValue()));
        }
        expanded = LiteralNode.unescape(expanded);

        if (configValue instanceof ConfigValueImpl) {
            ConfigValueImpl value = (ConfigValueImpl) configValue;
            return value.withValue(expanded);
        }
        // TODO Can we have the situation whe have ConfigValue that is not an Atbash one?
        return configValue;
    }


}
