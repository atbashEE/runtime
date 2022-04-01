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
package be.atbash.runtime.testing.jupiter;

import be.atbash.runtime.testing.AbstractAtbashTest;
import be.atbash.runtime.testing.AtbashContainer;
import com.github.dockerjava.api.exception.NotFoundException;
import jakarta.ws.rs.InternalServerErrorException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

public class ShowLogWhenFailedExceptionHandler implements TestExecutionExceptionHandler {

    @Override
    public void handleTestExecutionException(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        if (throwable instanceof AssertionError || throwable instanceof NotFoundException || throwable instanceof InternalServerErrorException) {
            AtbashContainer atbash = AbstractAtbashTest.atbash;
            String logs = atbash.getLogs();
            System.out.println("Atbash container log");
            System.out.println(logs);
        }
        throw throwable;  // rethrow. We just wanted to output the container log.
    }
}
