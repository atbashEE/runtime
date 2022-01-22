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
package be.atbash.runtime.testing.arquillian;

import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;

import java.util.Collections;
import java.util.Map;

public class TCKSniffer implements Sniffer {
    @Override
    public Specification[] detectedSpecifications() {
        return new Specification[]{Specification.TCK};
    }

    @Override
    public boolean triggered(Class<?> aClass) {
        // When TCK module is activated, this triggers make sure the Specification TCK is selected.
        return true;
    }

    @Override
    public boolean triggered(String descriptorName, String content) {
        // When TCK module is activated, this triggers make sure the Specification TCK is selected.
        return true;
    }

    @Override
    public boolean isFastDetection() {
        return true;
    }

    @Override
    public Map<String, String> deploymentData() {
        return Collections.EMPTY_MAP;
    }
}
