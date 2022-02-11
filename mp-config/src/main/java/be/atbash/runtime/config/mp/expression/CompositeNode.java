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

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.microprofile.config.inject.ConfigProperty.UNCONFIGURED_VALUE;

public class CompositeNode extends Node {

    public final static CompositeNode BLANK;

    static {
        BLANK = new CompositeNode();
        BLANK.addNode(new LiteralNode(UNCONFIGURED_VALUE));
    }

    private final List<Node> nodes = new ArrayList<>();

    public void addNode(Node node) {
        nodes.add(node);
    }

    public boolean isNotAnExpression() {
        return nodes.size() == 1 && nodes.get(0).isLiteral();
    }

    @Override
    public String evaluate(ConfigSourceInterceptorContext context) {
        StringBuilder result = new StringBuilder();
        for (Node node : nodes) {
            result.append(node.evaluate(context));
        }
        return result.toString();

    }
}
