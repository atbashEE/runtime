/*
 * Copyright 2021-2023 Rudy De Busscher (https://www.atbash.be)
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
import be.atbash.runtime.core.data.exception.message.ExceptionMessageUtil;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The Module Manager is responsible for starting and stopping all the modules.
 * There should never be an attempt to use the methods {@code startedModules}
 * and {@code stopModules} in a concurrent fashion.
 * It is allowed and possible to have multiple cycles of start and stop of all the modules (but is uses the same configuration)
 */
public final class ModuleManager {

    private static ModuleManager INSTANCE;
    private static final Logger LOGGER = LoggerFactory.getLogger(ModuleManager.class);

    private final ConfigurationParameters configurationParameters;

    private ExecutorService executorService;

    private static final Object MODULE_START_LOCK = new Object();
    // The next properties need to be thread safe and guarded by the MODULE_START_LOCK
    private final List<String> startedModuleNames = new CopyOnWriteArrayList<>();
    private final List<Module<?>> startedModules = new CopyOnWriteArrayList<>();
    private final List<String> installingModules = new CopyOnWriteArrayList<>();

    private String[] requestedModules;
    // A synchronizer to make sure all modules are started
    private CountDownLatch allModulesStarted;
    private final AtomicInteger numberOfStartsRunning = new AtomicInteger(0);

    private List<Module> modules;  // Only read, no need for synchronization.

    private boolean modulesStarted = false;
    private boolean moduleStartFailed = false;

    // The core objects but used several times in various methods, so we keep a reference here.
    private RunData runData;
    private WatcherService watcherService;
    private Deployer deployer;
    private Module<Object> coreModule;  // TODO Can we avoid this, to keep that reference?

    private final boolean traceModuleStartProcessing;

    private ModuleManager(ConfigurationParameters configurationParameters) {
        this.configurationParameters = configurationParameters;
        traceModuleStartProcessing = Boolean.parseBoolean(System.getProperty("traceModuleStartProcessing", "false"));
        ExceptionMessageUtil.addModule("core-data");  // initialize Exception messages for core-data module.
    }

    private boolean init() {
        executorService = Executors.newFixedThreadPool(5);

        modules = findAllModules();

        // Data Module must be the first one as everything else can be dependent on it.
        coreModule = ModuleUtil.findModule(modules, Module.CORE_MODULE_NAME);
        if (!startEssentialModule(coreModule, configurationParameters)) {
            return false;
        }

        // keep a reference to the core objects
        runData = coreModule.getRuntimeObject(RunData.class);
        watcherService = coreModule.getRuntimeObject(WatcherService.class);
        // Core module is interested in events.
        EventManager.getInstance().registerListener(coreModule);

        // Start Config module
        Module<Object> module2 = ModuleUtil.findModule(modules, Module.CONFIG_MODULE_NAME);
        if (!startEssentialModule(module2, this.configurationParameters)) {
            return false;
        }

        // get RuntimeConfiguration from Config Module.
        RuntimeConfiguration runtimeConfiguration = module2.getRuntimeObject(RuntimeConfiguration.class);

        // Start Logging
        Module<Object> module3 = ModuleUtil.findModule(modules, Module.LOGGING_MODULE_NAME);
        return startEssentialModule(module3, runtimeConfiguration);
    }

    private void registerSniffers() {
        SnifferManager snifferManager = SnifferManager.getInstance();
        startedModules.forEach(m -> snifferManager.registerSniffer(m.moduleSniffer()));
    }

    /**
     * Starts all the non-essential modules. Return false when a module is requested that doesn't exist.
     * It can also throw an {@link AtbashStartupAbortException} when a module fails to start.
     *
     * @return successful started all modules.
     */
    public boolean startModules() {
        if (modulesStarted) {
            // Don't start twice
            return true;
        }

        if (moduleStartFailed) {
            LOGGER.atError().log("MODULE-101");
            throw new AtbashStartupAbortException();
        }

        if (!init()) {
            throw new AtbashStartupAbortException();
        }

        RuntimeConfiguration runtimeConfiguration = RuntimeObjectsManager.getInstance().getExposedObject(RuntimeConfiguration.class);
        requestedModules = runtimeConfiguration.getRequestedModules();
        if (validateRequestedModules()) {
            try {
                allModulesStarted = new CountDownLatch(1);  // init the synchronizer.
                if (traceModuleStartProcessing) {
                    System.err.printf("Requested Modules %s%n", String.join(",", requestedModules));
                }
                findAndStartModules();

                runData.setStartedModules(startedModuleNames);
                registerSniffers();

                // Register deployer as Event Listener.
                List<Module> modulesCopy = new ArrayList<>(this.startedModules);
                deployer = new Deployer(watcherService, runtimeConfiguration, modulesCopy);
                EventManager.getInstance().registerListener(deployer);

                modulesStarted = true;
            } catch (Throwable t) {
                moduleStartFailed = true;
                throw t;
            } finally {
                clearExecutorService();
            }
            return true;
        } else {
            // validateRequestedModules() has already logged the error.
            moduleStartFailed = true;
            clearExecutorService();
            return false;
        }
    }

    private void traceModuleStartProcessing(String step) {
        String threadName = Thread.currentThread().getName();
        System.err.printf("Trace Module start [%s] - allModulesStarted %s - numberOfStartsRunning %s - %s%n"
                , threadName
                , allModulesStarted
                , numberOfStartsRunning
                , step);

    }

    private void clearExecutorService() {
        executorService.shutdown();  // No tasks should be waiting or running
        executorService = null;
    }

    private boolean validateRequestedModules() {
        List<String> moduleNames = modules.stream()
                .map(Module::name)
                .collect(Collectors.toList());
        List<String> unknownModules = Arrays.stream(requestedModules)
                .filter(n -> !moduleNames.contains(n))
                .collect(Collectors.toList());
        if (!unknownModules.isEmpty()) {
            Logger logger = LoggingUtil.getMainLogger(ModuleManager.class);
            logger.atError().addArgument(String.join(",", unknownModules)).log("CONFIG-012");
        }
        return unknownModules.isEmpty();
    }

    private void findAndStartModules() {
        List<String> modulesToStart;
        synchronized (MODULE_START_LOCK) {
            modulesToStart = Arrays.stream(requestedModules)
                    .filter(m -> canStart(m, startedModuleNames, installingModules))
                    .collect(Collectors.toList());
            installingModules.addAll(modulesToStart);

            if (traceModuleStartProcessing) {
                traceModuleStartProcessing(String.format("Modules to Start %s", modulesToStart));
            }

            if (modulesToStart.isEmpty() &&
                    (requestedModules.length > startedModuleNames.size()
                            || numberOfStartsRunning.get() > 0)) {

                if (traceModuleStartProcessing) {
                    traceModuleStartProcessing("Nothing to start for the moment, end Thread");
                }

                //We can't start a module for the moment but not all modules are running yet
                // or there is still some starter in progress.
                // So we need to wait for another thread to finish
                // and this thread can die.
                return;
            }

            if (modulesToStart.isEmpty() &&
                    requestedModules.length == startedModuleNames.size()
                    && numberOfStartsRunning.get() == 0) {

                if (traceModuleStartProcessing) {
                    traceModuleStartProcessing("Nothing to start anymore and everything is done. Release Countdown Latch");
                }

                allModulesStarted.countDown();
            }
        }
        Map<Module<Object>, Future<Boolean>> futures = modulesToStart
                .stream()
                .map(name -> ModuleUtil.findModule(modules, name))
                .collect(Collectors.toMap(Function.identity(), this::startModule));

        // Wait for the result of each started module.
        try {
            while (!futures.isEmpty()) {
                Iterator<Map.Entry<Module<Object>, Future<Boolean>>> entryIterator = futures.entrySet().iterator();
                while (entryIterator.hasNext()) {
                    Map.Entry<Module<Object>, Future<Boolean>> entry = entryIterator.next();
                    if (entry.getValue().isDone()) {

                        entryIterator.remove();
                        // This finish does the necessary bookkeeping and launches another round of launches
                        finishStartModule(entry.getKey(), entry.getValue().get());
                    }
                }
            }
        } catch (InterruptedException e) {
            // Keep thread interrupted for correct cleanup and closure.
            Thread.currentThread().interrupt();
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        } catch (ExecutionException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }

        try {
            // We wait until one of the other threads running this method says that there are no more modules to start.

            if (traceModuleStartProcessing) {
                traceModuleStartProcessing("Wait for the CountdownLatch ");
            }

            allModulesStarted.await();
        } catch (InterruptedException e) {
            // Keep thread interrupted for correct cleanup and closure.
            Thread.currentThread().interrupt();
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }
    }

    private Future<Boolean> startModule(Module<Object> module) {
        numberOfStartsRunning.incrementAndGet();
        if (traceModuleStartProcessing) {
            traceModuleStartProcessing("numberOfStartsRunning +1");
        }

        if (traceModuleStartProcessing) {
            traceModuleStartProcessing(String.format("Start module %s", module.name()));
        }

        // Register Exception messages for module before module starts.
        ExceptionMessageUtil.addModule(module.name());

        return executorService.submit(createModuleStarterThread(module, null));
    }

    private void finishStartModule(Module<Object> module, Boolean success) {
        if (traceModuleStartProcessing) {
            traceModuleStartProcessing(String.format("Finish Start module %s", module.name()));
        }

        // Module failed? Abort startup.
        if (success == null || !success) {
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

        numberOfStartsRunning.decrementAndGet();
        if (traceModuleStartProcessing) {
            traceModuleStartProcessing("numberOfStartsRunning -1");
        }


        if (traceModuleStartProcessing) {
            traceModuleStartProcessing("Launch another start round");
        }

        // More modules to start?
        executorService.submit(this::findAndStartModules);
    }

    private ModuleStarter createModuleStarterThread(Module<Object> module, Object configValue) {
        // ModuleStarter can keep track of a successful start of the module.
        ModuleStarter result = new ModuleStarter(module);
        // is config provided ?? (for the essential modules)
        if (configValue != null) {
            module.setConfig(configValue);
        } else {
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
        }
        return result;
    }

    private <T> boolean startEssentialModule(Module<Object> module, Object configValue) {
        // Register message exception bundle for essential module
        ExceptionMessageUtil.addModule(module.name());

        Future<Boolean> starter = executorService.submit(createModuleStarterThread(module, configValue));

        Boolean success;
        try {
            success = starter.get();
        } catch (InterruptedException e) {
            // Keep thread interrupted for correct cleanup and closure.
            Thread.currentThread().interrupt();
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        } catch (ExecutionException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }

        synchronized (MODULE_START_LOCK) {
            installingModules.remove(module.name());
            startedModuleNames.add(module.name());
            startedModules.add(module);
        }
        RuntimeObjectsManager.getInstance().register(module);

        return success;
    }

    private boolean canStart(String moduleName, List<String> startedModules, List<String> currentStarting) {
        // Not already started but all dependencies are started
        Module<?> module = ModuleUtil.findModule(modules, moduleName);
        return !containsModuleName(moduleName, startedModules)
                && !containsModuleName(moduleName, currentStarting)
                && Arrays.stream(module.dependencies())
                .allMatch(name -> containsModuleName(name, startedModules));
    }

    private boolean containsModuleName(String moduleName, List<String> moduleList) {
        // We don't need to check on exact matches, but startsWith.
        // mp-jwt depends on Jersey, but we have 'jersey' and 'jersey-se'. So correct startup
        // must be possible with either 'jersey' modules
        return moduleList.stream()
                .anyMatch(s -> s.startsWith(moduleName));
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
            LOGGER.atTrace().addArgument(moduleList).log("MODULE-1001");
        }
        return result;
    }

    public void stopModules() {
        if (!modulesStarted) {
            // Nothing to do
            return;
        }

        EventManager eventManager = EventManager.getInstance();
        new ArrayDeque<>(startedModules)
                .descendingIterator()
                .forEachRemaining(module -> {
                    eventManager.unregisterListener(module);
                    module.stop();
                });
        eventManager.unregisterListener(deployer);
        eventManager.unregisterListener(coreModule);

        watcherService.cleanup();

        modulesStarted = false;
        // don't set moduleStartFailed to false as it doesn't make sense to try again.
        startedModuleNames.clear();
        startedModules.clear();
    }

    /**
     * Initialize the Module Manager.  This already starts the essential modules (core, Config and Logging) and
     * can result in a {@link AtbashStartupAbortException} when a module fails to start.
     *
     * @param configurationParameters
     * @return
     */
    public static ModuleManager initModuleManager(ConfigurationParameters configurationParameters) {
        // FIXME Review these ConfigurationParameters as the Jakarta Runner does not use all of them.
        INSTANCE = new ModuleManager(configurationParameters);
        return INSTANCE;
    }

    public static ModuleManager getInstance() {
        if (INSTANCE == null) {
            throw new IncorrectUsageException("MODULE-002");
        }
        return INSTANCE;
    }

}
