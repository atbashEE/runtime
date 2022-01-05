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
package be.atbash.runtime.core.module;

import be.atbash.runtime.core.data.RunData;
import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.exception.AtbashStartupAbortException;
import be.atbash.runtime.core.data.exception.IncorrectUsageException;
import be.atbash.runtime.core.data.exception.UnexpectedException;
import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventManager;
import be.atbash.runtime.core.data.parameter.ConfigurationParameters;
import be.atbash.runtime.core.data.util.ModuleUtil;
import be.atbash.runtime.core.data.watcher.WatcherService;
import be.atbash.runtime.core.deployment.Deployer;
import be.atbash.runtime.core.deployment.SnifferManager;
import be.atbash.runtime.logging.LoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ModuleManager {

    private static ModuleManager INSTANCE;
    private static final Logger LOGGER = LoggerFactory.getLogger(ModuleManager.class);

    private ConfigurationParameters configurationParameters;

    private static final Object MODULE_START_LOCK = new Object();
    // The next properties need to be thread safe and guarded by the MODULE_START_LOCK
    private final List<String> startedModuleNames = new CopyOnWriteArrayList<>();
    private final List<Module<?>> startedModules = new CopyOnWriteArrayList<>();
    private final List<String> installingModules = new CopyOnWriteArrayList<>();

    private List<Module> modules;  // Only read, no need for synchronization.

    private boolean modulesStarted = false;

    // The core objects but used several times in various methods, so we keep a reference here.
    private RunData runData;
    private WatcherService watcherService;

    private ModuleManager(ConfigurationParameters configurationParameters) {
        this.configurationParameters = configurationParameters;

        if (!init(configurationParameters)) {
            throw new AtbashStartupAbortException();
        }

    }

    private boolean init(ConfigurationParameters configurationParameters) {
        modules = findAllModules();

        // Data Module must be the first one as everything else can be dependent on it.
        Module<?> coreModule = ModuleUtil.findModule(modules, Module.CORE_MODULE_NAME);
        if (!startEssentialModule(coreModule, configurationParameters.getWatcher())) {
            return false;
        }

        // keep a reference to the core objects
        runData = coreModule.getRuntimeObject(RunData.class);
        watcherService = coreModule.getRuntimeObject(WatcherService.class);
        // Core module is interested in events.
        EventManager.getInstance().registerListener(coreModule);

        // Start Config module
        Module<?> module2 = ModuleUtil.findModule(modules, Module.CONFIG_MODULE_NAME);
        if (!startEssentialModule(module2, this.configurationParameters)) {
            return false;
        }

        // get RuntimeConfiguration from Config Module.
        RuntimeConfiguration runtimeConfiguration = module2.getRuntimeObject(RuntimeConfiguration.class);

        // Start Logging
        Module<?> module3 = ModuleUtil.findModule(modules, Module.LOGGING_MODULE_NAME);
        if (!startEssentialModule(module3, runtimeConfiguration)) {
            return false;
        }

        return true;
    }

    private void registerSniffers() {
        SnifferManager snifferManager = SnifferManager.getInstance();
        startedModules.forEach(m -> snifferManager.registerSniffer(m.moduleSniffer()));
    }

    /**
     * Starts all the non-essential modules. Return false when a module is requested that doesn't exist.
     * It can also throw an {@link AtbashStartupAbortException} when a module fails to start.
     *
     * @return
     */
    public boolean startModules() {
        if (modulesStarted) {
            // Don't start twice
            return true;
        }
        modulesStarted = true;
        RuntimeConfiguration runtimeConfiguration = RuntimeObjectsManager.getInstance().getExposedObject(RuntimeConfiguration.class);
        String[] requestedModules = runtimeConfiguration.getRequestedModules();
        if (validateRequestedModules(requestedModules)) {
            findAndStartModules(requestedModules);

            runData.setStartedModules(startedModuleNames);
            registerSniffers();

            // Register deployer as Event Listener.
            List<Module> modulesCopy = new ArrayList<>(this.startedModules);
            EventManager.getInstance().registerListener(new Deployer(watcherService, runtimeConfiguration, modulesCopy));

            return true;
        } else {
            // validateRequestedModules() has already logged the error.
            return false;
        }
    }

    private boolean validateRequestedModules(String[] requestedModules) {
        List<String> moduleNames = modules.stream()
                .map(Module::name)
                .collect(Collectors.toList());
        List<String> unknownModules = Arrays.stream(requestedModules)
                .filter(n -> !moduleNames.contains(n))
                .collect(Collectors.toList());
        if (!unknownModules.isEmpty()) {
            Logger logger = LoggingUtil.getMainLogger(ModuleManager.class);
            logger.error(String.format("CONFIG-012: Incorrect Module name(s) specified '%s' (abort startup)", String.join(",", unknownModules)));
        }
        return unknownModules.isEmpty();
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
                .map(name -> ModuleUtil.findModule(modules, name))
                .forEach(module -> startModule(module, requestedModules));
    }

    private void startModule(Module<Object> module, String[] requestedModules) {
        // ModuleStarter can keep track of a successful start of the module.

        ModuleStarter starter = new ModuleStarter(module);
        Thread moduleStarterThread = new Thread(starter);
        // Does the module need configuration?
        Class<?> moduleConfigClass = module.getModuleConfigClass();
        if (moduleConfigClass != null) {
            Object exposedObject = RuntimeObjectsManager.getInstance().getExposedObject(moduleConfigClass);
            if (exposedObject == null) {
                throw new IllegalArgumentException(String.format("ModuleConfigClass %s not found for Module %s", moduleConfigClass, module.name()));
            }
            // Set the configuration
            module.setConfig(exposedObject);
        }

        // Start the Thread that start the module.
        moduleStarterThread.start();
        Thread.yield();  // So that other modules can start in parallel

        // Wait for the module to be started
        try {
            moduleStarterThread.join();
        } catch (InterruptedException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }
        // Module failed? Abort startup.
        if (!starter.isSuccess()) {
            throw new AtbashStartupAbortException();
        }
        // Some bookkeeping around modules.
        synchronized (MODULE_START_LOCK) {
            installingModules.remove(module.name());
            startedModules.add(module);
            startedModuleNames.add(module.name());
        }
        // Register Objects for this module
        RuntimeObjectsManager.getInstance().register(module);
        // Register module as event listener
        EventManager.getInstance().registerListener(module);

        // More modules to start?
        if (requestedModules != null) {
            findAndStartModules(requestedModules);
        }
    }

    private <T> boolean startEssentialModule(Module<T> module, Object configValue) {
        ModuleStarter starter = new ModuleStarter(module);
        Thread moduleStarterThread = new Thread(starter);
        module.setConfig((T) configValue);
        moduleStarterThread.start();
        Thread.yield();  // We have a generic ModuleStarter that runs in a separate Thread.
        // We are reusing that here so we make sure we give the change the other thread starts and we can wait for its completion.
        try {
            moduleStarterThread.join();
        } catch (InterruptedException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }
        synchronized (MODULE_START_LOCK) {
            installingModules.remove(module.name());
            startedModuleNames.add(module.name());
            startedModules.add(module);
        }
        RuntimeObjectsManager.getInstance().register(module);

        return starter.isSuccess();
    }

    private boolean canStart(String moduleName, List<String> startedModules, List<String> currentStarted) {
        // Not already started but all dependencies are started
        Module module = ModuleUtil.findModule(modules, moduleName);
        return !startedModules.contains(moduleName)
                && Arrays.stream(module.dependencies())
                .allMatch(startedModules::contains);
    }

    /**
     * Load all modules through the Service Loader mechanism
     *
     * @return
     */
    private List<Module> findAllModules() {
        ServiceLoader<Module> loader = ServiceLoader.load(Module.class);

        Iterator<Module> iterator = loader.iterator();
        List<Module> result = new ArrayList<>();
        while (iterator.hasNext()) {
            result.add(iterator.next());
        }
        if (LoggingUtil.isVerbose()) {
            String moduleList = result.stream().map(Module::name).collect(Collectors.joining(","));
            LOGGER.trace(String.format("MODULE-1001: List of Modules included in Runtime %s", moduleList));
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

    /**
     * Initialize the Module Manager.  This already starts the essential modules (core, Config and Logging) and
     * can result in a {@link AtbashStartupAbortException} when a module fails to start.
     *
     * @param configurationParameters
     * @return
     */
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
