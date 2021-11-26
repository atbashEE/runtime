/*
 * Copyright 2021 Rudy De Busscher (https://www.atbash.be)
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
package be.atbash.runtime.core.module;

import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.exception.IncorrectUsageException;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventManager;
import be.atbash.runtime.core.deployment.Deployer;
import be.atbash.runtime.core.deployment.SnifferManager;
import be.atbash.runtime.core.data.parameter.ConfigurationParameters;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ModuleManager {

    private static ModuleManager INSTANCE;

    private static final Object MODULE_START_LOCK = new Object();
    private ConfigurationParameters configurationParameters;
    private final List<String> startedModuleNames = new CopyOnWriteArrayList<>();
    private final List<Module<?>> startedModules = new CopyOnWriteArrayList<>();
    private List<String> installingModules = new CopyOnWriteArrayList<>();
    private List<Module> modules;  // Only read, no need for synchronization.

    private boolean modulesStarted = false;

    private ModuleManager(ConfigurationParameters configurationParameters) {
        this.configurationParameters = configurationParameters;

        init();

        // Register deployer as Event Listener.
        RuntimeConfiguration runtimeConfiguration = ExposedObjectsModuleManager.getInstance().getExposedObject(RuntimeConfiguration.class);
        List<Module> modulesCopy = new ArrayList<>(this.modules);
        EventManager.getInstance().registerListener(new Deployer(runtimeConfiguration, modulesCopy));
    }

    private void init() {
        modules = findAllModules();

        // Data Module must be the first one as everything else can be dependent on it.
        Module<?> module1 = findModule(Module.DATA_MODULE_NAME);
        startEssentialModule(module1, null);

        Module<?> module2 = findModule(Module.CONFIG_MODULE_NAME);
        startEssentialModule(module2, configurationParameters);

        Module<?> module3 = findModule(Module.LOGGING_MODULE_NAME);
        RuntimeConfiguration runtimeConfiguration = module3.getExposedObject(RuntimeConfiguration.class);
        startEssentialModule(module3, runtimeConfiguration);

        registerSniffers();
    }

    private void registerSniffers() {
        SnifferManager snifferManager = SnifferManager.getInstance();
        modules.forEach(m -> snifferManager.registerSniffer(m.moduleSniffer()));
    }

    public boolean startModules() {
        if (modulesStarted) {
            // Don't start twice
            return true;
        }
        modulesStarted = true;
        RuntimeConfiguration runtimeConfiguration = ExposedObjectsModuleManager.getInstance().getExposedObject(RuntimeConfiguration.class);
        findAndStartModules(runtimeConfiguration.getRequestedModules());
        return true;  // For future reference when a certain module fails to start.
    }

    private void findAndStartModules(String[] requestedModules) {
        List<String> modulesToStart;
        synchronized (MODULE_START_LOCK) {
            modulesToStart = Arrays.stream(requestedModules)
                    .filter(m -> canStart(m, startedModuleNames, installingModules))
                    .collect(Collectors.toList());
            installingModules.addAll(modulesToStart);

        }

        if (modulesToStart.isEmpty()) {
            return;
        }
        modulesToStart
                .stream()
                .map(this::findModule)
                .forEach(module -> startModule(module, requestedModules));
    }

    private Module<Object> findModule(String moduleName) {
        return modules.stream()
                .filter(m -> m.name().equals(moduleName))
                .findAny()
                .orElseThrow(() -> new IllegalStateException(String.format("Can't find module %s", moduleName)));  // FIXME We need to validate the modules names to see if they exist.
    }

    private void startModule(Module<Object> module, String[] requestedModules) {
        Thread moduleStarterThread = new Thread(new ModuleStarter(module));
        Class<?> moduleConfigClass = module.getModuleConfigClass();
        if (moduleConfigClass != null) {
            Object exposedObject = ExposedObjectsModuleManager.getInstance().getExposedObject(moduleConfigClass);
            if (exposedObject == null) {
                // FIXME Logging/Exception??
            }
            module.setConfig(exposedObject);
        }

        moduleStarterThread.start();
        Thread.yield();  // So that other modules can start in parallel
        try {
            moduleStarterThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();  // FIXME
        }
        synchronized (MODULE_START_LOCK) {
            installingModules.remove(module.name());
            startedModules.add(module);
            startedModuleNames.add(module.name());
        }
        ExposedObjectsModuleManager.getInstance().register(module);
        EventManager.getInstance().registerListener(module);
        if (requestedModules != null) {
            findAndStartModules(requestedModules);
        }
    }

    private <T> void startEssentialModule(Module<T> module, Object configValue) {
        Thread moduleStarterThread = new Thread(new ModuleStarter(module));
        module.setConfig((T)configValue);
        moduleStarterThread.start();
        Thread.yield();  // So that other modules can start in parallel
        try {
            moduleStarterThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();  // FIXME
        }
        synchronized (MODULE_START_LOCK) {
            installingModules.remove(module.name());
            startedModuleNames.add(module.name());
            startedModules.add(module);
        }
        ExposedObjectsModuleManager.getInstance().register(module);

    }

    private boolean canStart(String moduleName, List<String> startedModules, List<String> currentStarted) {
        // Not already started but all dependencies are started
        Module module = findModule(moduleName);
        return !startedModules.contains(moduleName)
                && Arrays.stream(module.dependencies())
                .allMatch(startedModules::contains);
    }

    private List<Module> findAllModules() {
        ServiceLoader<Module> loader = ServiceLoader.load(Module.class);
        //return loader.stream().map(ServiceLoader.Provider::get).collect(Collectors.toList());
        Iterator<Module> iterator = loader.iterator();
        List<Module> result = new ArrayList<>();
        while (iterator.hasNext()) {
            result.add(iterator.next());
        }
        return result;
    }

    public void stopModules() {
        if (!modulesStarted) {
            // Nothing to do
            return;
        }
        modulesStarted = false;

        new ArrayDeque<>(startedModules)
                .descendingIterator()
                .forEachRemaining(Module::stop);
    }

    public static ModuleManager initModuleManager(ConfigurationParameters configurationParameters) {
        INSTANCE = new ModuleManager(configurationParameters);
        return INSTANCE;
    }

    public static ModuleManager getInstance() {
        if (INSTANCE == null) {
            throw new IncorrectUsageException(IncorrectUsageException.IncorrectUsageCode._MM001, "ModuleManger is not properly configured through `getInstance(ConfigurationParameters)` call");
        }
        return INSTANCE;
    }

}
