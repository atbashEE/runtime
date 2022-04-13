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
package be.atbash.runtime.packager.maven;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;

import java.util.Collections;
import java.util.List;

/**
 *
 */
public class MavenHelper {

    public void addDependency(Model pomFile, String groupId, String artifactId, String version) {
        addDependency(pomFile, groupId, artifactId, version, null, Collections.emptyList());
    }

    public void addDependency(Model pomFile, String groupId, String artifactId, String version, List<String> exclusions) {
        addDependency(pomFile, groupId, artifactId, version, null, exclusions);
    }

    public void addDependency(Model pomFile, String groupId, String artifactId, String version, String scope, List<String> exclusions) {
        addDependency(pomFile, groupId, artifactId, version, scope, null, exclusions);
    }

    public void addDependency(Model pomFile, String groupId, String artifactId, String version, String scope, String type, List<String> exclusions) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        if (scope != null) {

            dependency.setScope(scope);
        }
        if (type != null) {
            dependency.setType(type);
        }
        if (!exclusions.isEmpty()) {
            exclusions.forEach(e -> {
                // FIXME have some checks we have the expected 'format'
                String[] parts = e.split(":");
                Exclusion exclusion = new Exclusion();
                exclusion.setGroupId(parts[0]);
                exclusion.setArtifactId(parts[1]);
                dependency.addExclusion(exclusion);
            });


        }
        pomFile.addDependency(dependency);
    }

}
