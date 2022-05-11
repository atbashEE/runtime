package be.atbash.runtime.core.data.deployment;

public class CurrentArchiveDeployment {

    private static final CurrentArchiveDeployment INSTANCE = new CurrentArchiveDeployment();

    private final ThreadLocal<ArchiveDeployment> threadLocalValue = new ThreadLocal<>();

    public void setCurrent(ArchiveDeployment archiveDeployment) {
        threadLocalValue.set(archiveDeployment);
    }

    public ArchiveDeployment getCurrent() {
        return threadLocalValue.get();
    }

    public void clear() {
        threadLocalValue.remove();
    }

    public static CurrentArchiveDeployment getInstance() {
        return INSTANCE;
    }
}
