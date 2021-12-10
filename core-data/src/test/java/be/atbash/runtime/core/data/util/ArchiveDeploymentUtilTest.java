/*
 * Copyright 2021 Rudy De Busscher (https://www.atbash.be)
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
package be.atbash.runtime.core.data.util;

import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArchiveDeploymentUtilTest {

    @Test
    void assignContextRoots() {

        List<ArchiveDeployment> archives = new ArrayList<>();
        archives.add(new ArchiveDeployment(new File("archive1.war")));
        archives.add(new ArchiveDeployment(new File("archive2.war")));
        ArchiveDeploymentUtil.assignContextRoots(archives, "root1,root2");

        assertThat(archives.get(0).getContextRoot()).isEqualTo("/root1");
        assertThat(archives.get(1).getContextRoot()).isEqualTo("/root2");
    }

    @Test
    void assignContextRoots_SingleValue() {

        List<ArchiveDeployment> archives = new ArrayList<>();
        archives.add(new ArchiveDeployment(new File("archive1.war")));
        ArchiveDeploymentUtil.assignContextRoots(archives, "root");

        assertThat(archives.get(0).getContextRoot()).isEqualTo("/root");
    }

    @Test
    void assignContextRoots_emptyValue() {

        List<ArchiveDeployment> archives = new ArrayList<>();
        archives.add(new ArchiveDeployment(new File("archive1.war")));
        archives.add(new ArchiveDeployment(new File("archive2.war")));
        ArchiveDeploymentUtil.assignContextRoots(archives, "");

        assertThat(archives.get(0).getContextRoot()).isEqualTo("/archive1");
        assertThat(archives.get(1).getContextRoot()).isEqualTo("/archive2");
    }
}