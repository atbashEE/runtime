package be.atbash.runtime.core.data;

import be.atbash.runtime.core.data.module.Module;
import be.atbash.runtime.core.data.module.event.EventPayload;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;

import java.util.List;

/**
 * This is a 'pseudo module' so that every other part of the runtime can access some basic information
 * on what is running at the moment.
 */
public class DataModule implements Module<Void> {

    private RunData runData;

    @Override
    public String name() {
        return Module.DATA_MODULE_NAME;
    }

    @Override
    public String[] dependencies() {
        return new String[0];
    }

    @Override
    public Specification[] provideSpecifications() {
        return new Specification[0];
    }

    @Override
    public Sniffer moduleSniffer() {
        return null;
    }

    @Override
    public List<Class<?>> getExposedTypes() {
        return List.of(RunData.class);
    }

    @Override
    public <T> T getExposedObject(Class<T> exposedObjectType) {
        if (exposedObjectType.equals(RunData.class)) {
            return (T) runData;
        }
        return null;
    }

    @Override
    public void onEvent(EventPayload eventPayload) {

    }

    @Override
    public void run() {
        // The run of the module only requires that we have an empty instance
        // of this instance that can be retrieved.
        runData = new RunData();
    }
}
