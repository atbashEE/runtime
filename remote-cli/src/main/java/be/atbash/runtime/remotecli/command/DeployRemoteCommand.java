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
package be.atbash.runtime.remotecli.command;

import be.atbash.runtime.common.command.data.CommandResponse;
import be.atbash.runtime.core.data.deployment.ArchiveDeployment;
import be.atbash.runtime.core.data.module.event.EventManager;
import be.atbash.runtime.core.data.module.event.Events;
import be.atbash.runtime.core.data.util.ArchiveDeploymentUtil;
import be.atbash.runtime.core.data.util.StringUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DeployRemoteCommand implements ServerRemoteCommand, HandleFileUpload {


    private final List<UploadData> uploadedFiles = new ArrayList<>();

    @Override
    public CommandResponse handleCommand(Map<String, String> options) {
        CommandResponse result = new CommandResponse();

        result.setSuccess(true);

        // FIXME option to do this asynchronous? (in another thread when we already
        // respond to the CLI)
        EventManager eventManager = EventManager.getInstance();

        List<ArchiveDeployment> deployments = uploadedFiles.stream().map(this::buildArchiveDeployment).collect(Collectors.toList());
        String contextRoots = options.get("contextroot");
        if (!validateCommandLine(deployments, contextRoots)) {
            result.setSuccess(false);
            result.setErrorMessage("RC-101: The number of context root values doesn't match the number of archives.");
            return result;
        }
        ArchiveDeploymentUtil.assignContextRoots(deployments, contextRoots);
        deployments.forEach(
                deployment ->
                        eventManager.publishEvent(Events.DEPLOYMENT, deployment)
        );

        deployments.forEach(
                deployment -> {
                    String msg = String.format("Application deployed with the context '%s'", deployment.getContextRoot());
                    result.addData(deployment.getDeploymentName(), msg);
                });


        return result;

    }

    private ArchiveDeployment buildArchiveDeployment(UploadData data) {
        String name = StringUtil.determineDeploymentName(data.getName());
        return new ArchiveDeployment(data.getTempFileLocation(), name);
    }

    @Override
    public void uploadedFile(String name, InputStream inputStream) {

        try {
            uploadedFiles.add(new UploadData(name, storeStreamToTempFile(inputStream)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static File storeStreamToTempFile(InputStream in) throws IOException {
        final File tempFile = File.createTempFile("tmp_deploy_", "");
        tempFile.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            in.transferTo(out);
        }
        return tempFile;
    }

    private static boolean validateCommandLine(List<ArchiveDeployment> deployments, String contextRoots) {
        if (contextRoots.isBlank()) {
            // No contextroot value specified, nothing to check.
            return true;
        }

        String[] parts = contextRoots.split(",");
        return deployments.size() == parts.length;
    }

    private static class UploadData {
        private String name;
        private File tempFileLocation;

        public UploadData(String name, File tempFileLocation) {
            this.name = name;
            this.tempFileLocation = tempFileLocation;
        }

        public String getName() {
            return name;
        }

        public File getTempFileLocation() {
            return tempFileLocation;
        }
    }
}
