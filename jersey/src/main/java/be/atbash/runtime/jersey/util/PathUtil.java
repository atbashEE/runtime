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
package be.atbash.runtime.jersey.util;

public final class PathUtil {

    private PathUtil() {
    }

    public static String determinePathForServlet(String applicationPath) {
        // We add / in front of the application path to make sure we have it
        // The while loop will remove it hen it was already specified by suer.
        StringBuilder result = new StringBuilder("/" + applicationPath.trim() + "/*");
        int idx = 1;
        while (idx < result.length() - 1) {
            if (result.charAt(idx - 1) == result.charAt(idx)
                    && '/' == result.charAt(idx)) {
                result.deleteCharAt(idx);
            } else {
                idx++;
            }
        }
        return result.toString();
    }
}
