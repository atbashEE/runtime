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

public class Expressions {

    public static CompositeNode parseExpression(String text) {
        CompositeNode result = new CompositeNode();
        char[] chars = text.toCharArray();
        boolean withinExpression = false;
        boolean defaultValueStarted = false;
        int startSequence = 0;
        int nestedExpressionLevel = 0;
        for (int i = 0; i < chars.length; i++) {
            if (isStartExpression(chars, i)) {
                if (defaultValueStarted) {
                    nestedExpressionLevel++;
                    continue;
                }
                if (!withinExpression) {
                    if (i != startSequence) {
                        result.addNode(new LiteralNode(text.substring(startSequence, i)));
                    }
                    withinExpression = true;
                    startSequence = i;
                    continue;
                } else {
                    nestedExpressionLevel++;
                }
            }

            if (withinExpression && isColonStartDefaultValue(chars, i)) {
                defaultValueStarted = true;
            }
            if (withinExpression && isEndExpression(chars, i)) {
                if (nestedExpressionLevel > 0) {
                    nestedExpressionLevel--;
                    continue;
                }
                result.addNode(new ExpressionNode(text.substring(startSequence + 2, i)));
                withinExpression = false;
                defaultValueStarted = false;

                startSequence = i + 1;
            }
        }
        if (!withinExpression && startSequence < text.length()) {
            result.addNode(new LiteralNode(text.substring(startSequence)));
        }
        return result;
    }

    private static boolean isColonStartDefaultValue(char[] chars, int position) {
        if (chars[position] == ':') {
            if (position > 2) {
                return !(chars[position - 1] == '\\' && chars[position - 2] == '\\');
            }
        }
        return false;

    }

    private static boolean isEndExpression(char[] chars, int position) {
        if (chars[position] == '}') {
            if (position > 1) {
                return chars[position - 1] != '\\' ;
            }
        }
        return false;
    }

    private static boolean isStartExpression(char[] chars, int position) {
        if (chars[position] == '$') {
            if (position < 1) {
                // No room for escaping
                return true;
            }
            return chars[position - 1] != '\\';
        }

        return false;
    }


}
