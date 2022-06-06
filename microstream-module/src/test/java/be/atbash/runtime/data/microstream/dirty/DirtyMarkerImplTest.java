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
package be.atbash.runtime.data.microstream.dirty;

import be.atbash.runtime.data.microstream.Pojo;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DirtyMarkerImplTest {

    @Mock
    private DirtyInstanceCollector collectorMock;

    @InjectMocks
    private DirtyMarkerImpl dirtyMarker;

    @Test
    void mark() {
        Pojo pojo = new Pojo();
        dirtyMarker.mark(pojo).setValue("JUnit");

        Assertions.assertThat(pojo.getValue()).isEqualTo("JUnit");
        Mockito.verify(collectorMock).addInstance(pojo);
    }
}