package be.atbash.runtime.core.data;

import be.atbash.runtime.core.data.deployment.ArchiveDeployment;

import java.util.ArrayList;
import java.util.List;

public class RunData {

    private final List<ArchiveDeployment> deployments = new ArrayList<>();

    public void deployed(ArchiveDeployment deployment) {
        deployments.add(deployment);
    }

    public List<ArchiveDeployment> getDeployments() {
        return deployments;
    }
}
