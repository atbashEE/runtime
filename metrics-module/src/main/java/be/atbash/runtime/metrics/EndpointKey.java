/*
 * Copyright 2021-2023 Rudy De Busscher (https://www.atbash.be)
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
package be.atbash.runtime.metrics;

public class EndpointKey {

    private String deploymentName;
    private String path;

    public EndpointKey(String deploymentName, String path) {
        this.deploymentName = deploymentName;
        this.path = path;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public String getPath() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EndpointKey that = (EndpointKey) o;

        if (!deploymentName.equals(that.deploymentName)) {
            return false;
        }
        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        int result = deploymentName.hashCode();
        result = 31 * result + path.hashCode();
        return result;
    }
}
