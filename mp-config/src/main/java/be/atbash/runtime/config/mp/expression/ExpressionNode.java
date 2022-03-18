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

import be.atbash.runtime.config.mp.sources.interceptor.ConfigSourceInterceptorContext;
import org.eclipse.microprofile.config.ConfigValue;

public class ExpressionNode extends Node {

    private final CompositeNode key;
    private final CompositeNode defaultValue;

    public ExpressionNode(String expressionValue) {
        int colonPosition = checkForDefaultValue(expressionValue);
        if (colonPosition < 0) {
            key = Expressions.parseExpression(expressionValue);
            defaultValue = CompositeNode.BLANK;
        } else {
            key = Expressions.parseExpression(expressionValue.substring(0, colonPosition));
            defaultValue = Expressions.parseExpression(expressionValue.substring(colonPosition + 1));

        }
    }

    private int checkForDefaultValue(String literalValue) {
        int result = -1;
        int pos = -1;
        boolean stop = false;

        while (!stop) {
            pos = literalValue.indexOf(':', pos + 1);
            if (pos == -1) {
                stop = true;
            } else {
                if (isColon(literalValue, pos)) {
                    result = pos;
                    stop = true;
                }
            }

        }
        return result;
    }

    private boolean isColon(String value, int pos) {
        if (pos < 2) {
            return true;
        }
        return value.charAt(pos - 2) != '\\' || value.charAt(pos - 1) != '\\';
    }

    @Override
    public String evaluate(ConfigSourceInterceptorContext context) {

        String keyValue = key.evaluate(context);
        return resolve(keyValue, defaultValue, context);

    }

    private String resolve(String key, CompositeNode defaultValue, ConfigSourceInterceptorContext context) {
        ConfigValue configValue = context.proceed(key);
        if (configValue == null) {
            return defaultValue.evaluate(context);
        } else {
            return configValue.getRawValue();
        }
    }
}
