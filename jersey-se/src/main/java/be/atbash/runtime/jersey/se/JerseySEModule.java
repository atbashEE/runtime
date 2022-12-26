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
package be.atbash.runtime.jersey.se;

import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.Specification;
import be.atbash.runtime.core.data.deployment.ApplicationExecution;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventPayload;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.runtime.core.data.util.StringUtil;
import be.atbash.runtime.core.data.watcher.WatcherService;
import be.atbash.runtime.core.module.RuntimeObjectsManager;
import be.atbash.util.reflection.ClassUtils;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.SeBootstrap;
import jakarta.ws.rs.core.Application;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class JerseySEModule implements Module<RuntimeConfiguration> {

    public static final String JERSEY_MODULE_NAME = "jersey-se";

    private static final Logger LOGGER = LoggerFactory.getLogger(JerseySEModule.class);

    private RuntimeConfiguration configuration;

    @Override
    public String name() {
        return JERSEY_MODULE_NAME;
    }

    @Override
    public String[] dependencies() {
        return new String[]{};
    }

    @Override
    public List<Class<?>> getRuntimeObjectTypes() {
        return Collections.emptyList();
    }

    @Override
    public <T> T getRuntimeObject(Class<T> exposedObjectType) {
        return null;
    }

    @Override
    public Specification[] provideSpecifications() {
        return new Specification[]{Specification.REST};
    }

    @Override
    public Class<? extends Sniffer> moduleSniffer() {
        return null;
    }

    @Override
    public void onEvent(EventPayload eventPayload) {

    }

    @Override
    public void executeDeployment(ApplicationExecution applicationExecution) {
        SeBootstrap.Configuration.Builder configBuilder = SeBootstrap.Configuration.builder();
        ResourceConfig resourceConfig = new ResourceConfig();

        handleResourcesDefinition(resourceConfig, applicationExecution);

        configBuilder.property(SeBootstrap.Configuration.PROTOCOL, "HTTP")
                .property(SeBootstrap.Configuration.HOST, applicationExecution.getHost())
                .property(SeBootstrap.Configuration.PORT, applicationExecution.getPort())
                .property(SeBootstrap.Configuration.ROOT_PATH, applicationExecution.getRoot());

        applicationExecution.getDeploymentData().put(JerseySEModuleConstant.APPLICATION_PATH, applicationExecution.getRoot());

        SeBootstrap.start(resourceConfig, configBuilder.build());

        LOGGER.atInfo().addArgument(applicationExecution.getDeploymentName()).log("JERSEY-104");
    }

    private void handleResourcesDefinition(ResourceConfig resourceConfig, ApplicationExecution applicationExecution) {

        resourceConfig.register(RuntimeApplicationEventListener.class);
        for (Class<?> someClass : applicationExecution.getResources()) {
            ResourceType resourceType = ResourceTypeUtil.determineType(someClass);
            switch (resourceType) {

                case APPLICATION:
                    updateContextPathForApplicationPath(someClass, applicationExecution);
                    Application application = ClassUtils.newInstance(someClass);

                    Set<Class<?>> resources = application.getClasses();
                    if (resources.isEmpty()) {
                        // Register the package of the Application
                        resourceConfig.packages(someClass.getPackageName());
                    } else {
                        // Register each resource/class defined in Application
                        resources.forEach(resourceConfig::register);
                    }

                    break;
                case RESOURCE:
                    // A resource with @Path or @Provider
                    resourceConfig.register(someClass);
                    break;
                case CLASS:
                    // Some class, use the package of it for scanning
                    resourceConfig.packages(someClass.getPackageName());
                    break;
                default:
                    throw new IllegalArgumentException(String.format("The value %s for enum ResourceType is unexpected.", resourceType));
            }
        }
    }

    private void updateContextPathForApplicationPath(Class<?> someClass, ApplicationExecution applicationExecution) {
        // @ApplicationPath is not picked up when providing Application as class.
        ApplicationPath applicationPath = someClass.getAnnotation(ApplicationPath.class);
        if (applicationPath != null) {
            String path = StringUtil.sanitizePath(applicationPath.value());
            if (applicationExecution.getRoot().equals("/")) {
                applicationExecution.setRoot(path);
            } else {
                applicationExecution.setRoot(applicationExecution.getRoot() + path);
            }
        }
    }

    @Override
    public void run() {

        WatcherService watcherService = RuntimeObjectsManager.getInstance().getExposedObject(WatcherService.class);
        watcherService.logWatcherEvent(JERSEY_MODULE_NAME, "JERSEY-1001: Module startup", false);

        //FIXME  Run of this module actually doesn't run anyhting

        watcherService.logWatcherEvent(JERSEY_MODULE_NAME, "JERSEY-1002: Module ready", false);

    }

    @Override
    public Class<RuntimeConfiguration> getModuleConfigClass() {
        return RuntimeConfiguration.class;
    }

    @Override
    public void setConfig(RuntimeConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void stop() {


    }
}
