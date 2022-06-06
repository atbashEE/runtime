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
package be.atbash.runtime.data.microstream.jaxrs;

import be.atbash.runtime.data.microstream.InstanceData;
import be.atbash.runtime.data.microstream.InstanceStorer;
import be.atbash.runtime.data.microstream.dirty.DirtyInstanceCollector;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

@Provider
public class DirtyCheckContainerResponseFilter implements ContainerResponseFilter {

    @Inject
    private DirtyInstanceCollector collector;

    @Inject
    private InstanceStorer instanceStorer;

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        if (!collector.getDirtyInstances().isEmpty()) {
            for (Object dirtyInstance : collector.getDirtyInstances()) {
                instanceStorer.queueForProcessing(new InstanceData(dirtyInstance, true));
            }

        }
    }
}
