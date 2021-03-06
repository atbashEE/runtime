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
package be.atbash.runtime.core.data;

import java.util.Arrays;
import java.util.Optional;

public enum Specification {

    // TCK is here as specification so that we can have a Sniffer that 'detects' it
    // and it can be used to determine the Deployer.
    HTML, SERVLET, REST, TCK;

    public static Specification fromCode(String value) {
        Optional<Specification> specification = Arrays.stream(Specification.values())
                .filter(s -> s.name().equalsIgnoreCase(value))
                .findAny();
        return specification.orElse(null);
    }
}
