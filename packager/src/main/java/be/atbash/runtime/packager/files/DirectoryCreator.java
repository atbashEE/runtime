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
package be.atbash.runtime.packager.files;

import be.atbash.runtime.packager.exception.DirectoryCreationException;

import java.io.File;

/**
 *
 */
public class DirectoryCreator {

    public boolean existsDirectory(File directory) {
        return directory.exists();
    }

    public void createDirectory(String directory) {
        createDirectory(new File(directory));
    }

    public void createDirectory(File directory) {
        boolean success = true;
        if (!directory.exists()) {
            success = directory.mkdirs();
        }
        if (!success) {
            String message = String.format("Unable to create directory '%s'", directory.getAbsoluteFile());
            throw new DirectoryCreationException(message);

        }
    }

}
