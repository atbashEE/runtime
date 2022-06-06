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
package be.atbash.runtime.data.microstream.exception;


public class StorageTypeException extends RuntimeException {
    // TODO Define message through resource bundle
    private static final String MESSAGE = "There is an incompatibility between the entity and the"
            + " current root in the StorageManager. Please check the compatibility. "
            + "Entity: %s and current root class %s";


    public <T, E> StorageTypeException(Class<T> entity, Class<E> root) {
        super(String.format(MESSAGE, entity, root));
    }
}
