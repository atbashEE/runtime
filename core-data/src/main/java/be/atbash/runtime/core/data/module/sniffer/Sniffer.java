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
package be.atbash.runtime.core.data.module.sniffer;

import be.atbash.runtime.core.data.Specification;

import java.util.Map;

public interface Sniffer {

    Specification[] detectedSpecifications();

    boolean triggered(Class<?> aClass);

    boolean triggered(String descriptorName, String content);

    /**
     * If triggered, is the sniffer used for other classes?
     * @return
     */
    boolean isFastDetection();

    /**
     * Returns the key value pairs determined by the sniffer that can be used by the Deployer or other
     * concept.  It contains for example the mapping of the Rest Servlet defined by @ApplicationPath.
     * @return An empty map or the data that needs to be stored for the deployment in the persisted deployment info
     */
    Map<String, String> deploymentData();
}
