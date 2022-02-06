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
package be.atbash.runtime.config.mp.sources.interceptor;

/**
 * A collection of built-in priority constants for {@link ConfigSourceInterceptor} that are supposed to be
 * ordered based on their {@code jakarta.annotation.Priority} class-level annotation.
 */
public final class Priorities {
    /**
     * Range for early interceptors defined by Platform specifications.
     */
    public static final int PLATFORM = 1000;

    /**
     * Range for interceptors defined by Atbash Config or Extension Libraries.
     */
    public static final int LIBRARY = 3000;

    /**
     * Range for interceptors defined by User Applications.
     */
    public static final int APPLICATION = 5000;

    private Priorities() {

    }
}
