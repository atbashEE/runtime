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
package be.atbash.runtime.core.data.util;

import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.exception.UnexpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

public final class ArchiveDeploymentUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveDeploymentUtil.class);

    private ArchiveDeploymentUtil() {
    }

    public static void assignContextRoots(List<ArchiveDeployment> archives, String contextRoot) {
        if (contextRoot.isBlank()) {
            return;
        }
        String[] rootValues = contextRoot.split(",");
        for (int i = 0; i < rootValues.length; i++) {
            archives.get(i).setContextRoot(rootValues[i]);
        }
    }
    public static boolean testOnArchive(File archiveFile, boolean isWar) {
        if (archiveFile == null) {
            return true;
        }
        boolean result = archiveFile.exists();
        if (!result) {
            LOGGER.warn(String.format("DEPLOY-105: file %s not found", archiveFile));
        }
        if (result && isWar) {
            try {
                result = archiveFile.getCanonicalPath().endsWith(".war");
                if (!result) {
                    LOGGER.warn(String.format("DEPLOY-106: Archive file %s is not a war file", archiveFile));
                }

            } catch (IOException e) {
                throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
            }
        }
        return result;

    }
}
