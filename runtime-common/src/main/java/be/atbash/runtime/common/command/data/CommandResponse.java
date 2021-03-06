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
package be.atbash.runtime.common.command.data;

import java.util.HashMap;
import java.util.Map;

public class CommandResponse {

    private boolean success = true;
    private String errorMessage;
    private final Map<String, String> data = new HashMap<>();

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * When setting error message, we also set the success flag to false
     * @param errorMessage
     */
    public void setErrorMessage(String errorMessage) {
        if (errorMessage != null) {
            this.errorMessage = errorMessage;
            this.success = false;
        }
    }

    public Map<String, String> getData() {
        return data;
    }

    public void addData(String key, String value) {
        data.put(key, value);
    }
}
