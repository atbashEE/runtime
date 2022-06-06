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
package be.atbash.runtime.data.microstream.cdi;


import be.atbash.runtime.data.microstream.InstanceData;
import be.atbash.runtime.data.microstream.InstanceStorer;
import be.atbash.runtime.data.microstream.dirty.DirtyInstanceCollector;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;


@Store
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class StoreInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(StoreInterceptor.class);

    @Inject
    private DirtyInstanceCollector collector;

    @Inject
    private InstanceStorer instanceStorer;

    @AroundInvoke
    public Object store(InvocationContext context) throws Exception {

        Object result = context.proceed();

        Store store = Optional.ofNullable(context.getMethod().getAnnotation(Store.class)).orElse(
                context.getMethod().getDeclaringClass().getAnnotation(Store.class));

        for (Object dirtyInstance : collector.getDirtyInstances()) {
            LOGGER.atDebug().addArgument(dirtyInstance.getClass().getName()).log("Storing object type {}");
            if (store.asynchronous()) {
                instanceStorer.queueForProcessing(new InstanceData(dirtyInstance, store.clearLazy()));
            } else {
                instanceStorer.storeChanged(dirtyInstance, store.clearLazy());
            }
        }

        collector.processedInstances();

        return result;
    }
}
