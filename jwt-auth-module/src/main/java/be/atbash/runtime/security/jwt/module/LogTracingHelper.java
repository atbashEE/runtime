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
package be.atbash.runtime.security.jwt.module;

import jakarta.ws.rs.container.ContainerRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class LogTracingHelper {

    private static final LogTracingHelper INSTANCE = new LogTracingHelper();

    private static final Logger LOGGER = LoggerFactory.getLogger(LogTracingHelper.class);
    public static final String MDC_KEY_REQUEST_ID = "RequestId";

    private final ThreadLocal<Boolean> logTracingActive = new ThreadLocal<>();
    private final Map<String, Boolean> contextRootTracingActive = new HashMap<>();

    private LogTracingHelper() {

    }

    public void storeLogTracingActive(String contextRoot, boolean active) {
        contextRootTracingActive.put(contextRoot, active);
    }

    public void startTracing(ContainerRequestContext context) {
        String contextRoot = getContextRoot(context.getUriInfo().getRequestUri().getPath());
        MDC.put(MDC_KEY_REQUEST_ID, UUID.randomUUID().toString());
        logTracingActive.set(contextRootTracingActive.getOrDefault(contextRoot, Boolean.FALSE));
    }

    public void stopTracing() {
        logTracingActive.remove();
        MDC.remove(MDC_KEY_REQUEST_ID);
    }

    public boolean isLogTracingActive() {
        return logTracingActive.get();
    }

    /**
     * Log information about request and log message parameters are passed directly. Use the
     * variant with a Supplier if the retrieval of the log parameters is more costly.
     * @param message
     * @param parameters
     */
    public void logTraceMessage(String message, Object... parameters) {
        if (logTracingActive.get()) {
            String traceMessage = String.format(message, parameters);
            LOGGER.atInfo()
                    .log("JWT-050", traceMessage);
        }
    }

    /**
     * Log information about request but log message parameters are retrieved lazily.
     * @param message
     * @param parameters
     */
    public void logTraceMessage(String message, Supplier<Object[]> parameters) {
        if (logTracingActive.get()) {
            String traceMessage = String.format(message, parameters.get());
            LOGGER.atInfo()
                    .log("JWT-050", traceMessage);
        }
    }


    private String getContextRoot(String path) {
        int idx = path.indexOf('/', 1);
        return path.substring(0, idx);
    }

    public static LogTracingHelper getInstance() {
        return INSTANCE;
    }
}
