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
package be.atbash.runtime.core.module;

import be.atbash.runtime.core.data.exception.AtbashStartupAbortException;
import be.atbash.runtime.core.data.exception.ExceptionHelper;
import be.atbash.runtime.core.data.module.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * Runnable that starts the {@link Module}.
 */
public class ModuleStarter implements Callable<Boolean> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModuleStarter.class);

    private final Module module;

    public ModuleStarter(Module module) {
        this.module = module;
    }

    @Override
    public Boolean call() throws Exception {
        try {
            module.run();
            return true;
        } catch (Throwable e) {
            if (!(e instanceof AtbashStartupAbortException)) {
                LOGGER.error(e.getMessage() + "\n" + ExceptionHelper.traceCaller(e, 5));
            }
            return false;
        }

    }
}
