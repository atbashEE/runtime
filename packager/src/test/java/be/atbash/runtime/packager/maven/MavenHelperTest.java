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

import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

class MavenHelperTest {

    @Test
    void addDependency() {
        MavenHelper helper = new MavenHelper();

        Model pomFile = new Model();

        helper.addDependency(pomFile, "org.group", "my.artifact", "1.0");

        Assertions.assertThat(pomFile.getDependencies()).hasSize(1);
        Assertions.assertThat(pomFile.getDependencies().get(0)).hasToString("Dependency {groupId=org.group, artifactId=my.artifact, version=1.0, type=jar}");
        Assertions.assertThat(pomFile.getDependencies().get(0).getScope()).isNull();
        Assertions.assertThat(pomFile.getDependencies().get(0).getExclusions()).isEmpty();
    }

    @Test
    void addDependency_asTest() {
        MavenHelper helper = new MavenHelper();

        Model pomFile = new Model();

        helper.addDependency(pomFile, "org.group", "my.artifact", "1.0", "test", Collections.emptyList());

        Assertions.assertThat(pomFile.getDependencies()).hasSize(1);
        Assertions.assertThat(pomFile.getDependencies().get(0)).hasToString("Dependency {groupId=org.group, artifactId=my.artifact, version=1.0, type=jar}");
        Assertions.assertThat(pomFile.getDependencies().get(0).getScope()).isEqualTo("test");
        Assertions.assertThat(pomFile.getDependencies().get(0).getExclusions()).isEmpty();
    }

    @Test
    void addDependency_withType() {
        MavenHelper helper = new MavenHelper();

        Model pomFile = new Model();

        helper.addDependency(pomFile, "org.group", "my.artifact", "1.0", "compile", "pom", Collections.emptyList());

        Assertions.assertThat(pomFile.getDependencies()).hasSize(1);
        Assertions.assertThat(pomFile.getDependencies().get(0)).hasToString("Dependency {groupId=org.group, artifactId=my.artifact, version=1.0, type=pom}");
        Assertions.assertThat(pomFile.getDependencies().get(0).getScope()).isEqualTo("compile");
        Assertions.assertThat(pomFile.getDependencies().get(0).getExclusions()).isEmpty();
    }

    @Test
    void addDependency_withExclusion() {
        MavenHelper helper = new MavenHelper();

        Model pomFile = new Model();

        helper.addDependency(pomFile, "org.group", "my.artifact", "1.0", List.of("group1:artifact1"));

        Assertions.assertThat(pomFile.getDependencies()).hasSize(1);
        Assertions.assertThat(pomFile.getDependencies().get(0)).hasToString("Dependency {groupId=org.group, artifactId=my.artifact, version=1.0, type=jar}");
        Assertions.assertThat(pomFile.getDependencies().get(0).getScope()).isNull();
        Assertions.assertThat(pomFile.getDependencies().get(0).getExclusions()).hasSize(1);
        Exclusion exclusion = pomFile.getDependencies().get(0).getExclusions().get(0);
        Assertions.assertThat(exclusion.getGroupId()).isEqualTo("group1");
        Assertions.assertThat(exclusion.getArtifactId()).isEqualTo("artifact1");
    }

    @Test
    void addDependency_withExclusion2() {
        MavenHelper helper = new MavenHelper();

        Model pomFile = new Model();

        helper.addDependency(pomFile, "org.group", "my.artifact", "1.0", List.of("group1:artifact1", "group2:artifact2"));

        Assertions.assertThat(pomFile.getDependencies()).hasSize(1);
        Assertions.assertThat(pomFile.getDependencies().get(0)).hasToString("Dependency {groupId=org.group, artifactId=my.artifact, version=1.0, type=jar}");
        Assertions.assertThat(pomFile.getDependencies().get(0).getScope()).isNull();
        Assertions.assertThat(pomFile.getDependencies().get(0).getExclusions()).hasSize(2);


        testExclusion(pomFile.getDependencies().get(0).getExclusions().get(0), "group1", "artifact1");
        testExclusion(pomFile.getDependencies().get(0).getExclusions().get(1), "group2", "artifact2");
    }

    private void testExclusion(Exclusion exclusion, String group, String artifact) {
        Assertions.assertThat(exclusion.getGroupId()).isEqualTo(group);
        Assertions.assertThat(exclusion.getArtifactId()).isEqualTo(artifact);
    }
}