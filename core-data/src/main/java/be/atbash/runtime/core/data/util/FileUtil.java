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

import java.io.*;
import java.util.logging.ErrorManager;
import java.util.zip.GZIPOutputStream;

public final class FileUtil {

    private static final String GZIP_EXTENSION = ".gz";

    private FileUtil() {
    }

    public static String getTempDirectory() {
        return System.getProperty("java.io.tmpdir");
    }

    public static File storeStreamToTempFile(InputStream in) throws IOException {
        final File tempFile = File.createTempFile("tmp_deploy_", ".war");
        tempFile.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            in.transferTo(out);
        }
        return tempFile;
    }

    public static boolean gzipFile(File infile) {

        boolean status = false;

        try (
                FileInputStream fis = new FileInputStream(infile);
                FileOutputStream fos = new FileOutputStream(infile.getCanonicalPath() + GZIP_EXTENSION);
                GZIPOutputStream gzos = new GZIPOutputStream(fos)
        ) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                gzos.write(buffer, 0, len);
            }
            gzos.finish();

            status = true;

        } catch (IOException ix) {
            new ErrorManager().error("Error gzipping log file", ix, ErrorManager.GENERIC_FAILURE);
        }

        return status;
    }
}
