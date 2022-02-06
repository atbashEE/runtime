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

public class LiteralNode extends Node {

    private static final String ESCAPABLES = "${}:";

    private final String literalValue;

    public LiteralNode(String literalValue) {
        this.literalValue = literalValue;
    }

    @Override
    public String evaluate(ConfigSourceInterceptorContext context) {
        // Don't perform the unescape at this level. It might make a literal an expression and do to recursion
        //tries to resolve the expression (but it was escaped!)
        return literalValue;
    }

    public static String unescape(String literalValue) {
        StringBuilder result = new StringBuilder();
        char[] chars = literalValue.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (!(chars[i] == '\\' && i < chars.length - 1 && ESCAPABLES.indexOf(chars[i + 1]) != -1)) {
                // Skip \ if followed by $, {, }, or :
                result.append(chars[i]);
            }

        }
        return result.toString();
    }
}
