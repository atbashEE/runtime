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
package be.atbash.runtime.testing.images;

import be.atbash.runtime.testing.model.ServerAdapterMetaData;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.nio.file.Path;

public class LoggableImageFromDockerFile extends ImageFromDockerfile {

    private final ServerAdapterMetaData metaData;
    private String baseImage = "???";

    public LoggableImageFromDockerFile(String dockerImageName, ServerAdapterMetaData metaData) {
        super(dockerImageName);
        this.metaData = metaData;
    }

    @Override
    public ImageFromDockerfile withDockerfile(Path dockerfile) {

        defineBaseImage(dockerfile);

        return super.withDockerfile(dockerfile);
    }

    // TODO We do not have any logging when Docker starts to download a new image.
    //  This can take a while so best to have some logging.
    //  But not yet determined how it can be done.
    //  It is part of the 'docker build' which is performed by org.testcontainers.images.builder.ImageFromDockerfile.resolve

    private void defineBaseImage(Path dockerfile) {
        // We can determine base image from metadata, no need to parse DockerFile
        baseImage = String.format("%s - %s - %s", getName(), metaData.getRuntimeVersion(), metaData.getJdkRuntime().name());
    }


    private String getName() {
        String result;
        switch (metaData.getRuntimeType()) {

            case DEFAULT:
                result = "default";
                break;
            case CUSTOM:
                result = metaData.getCustomImageName();
                break;
            default:
                throw new IllegalArgumentException("Value " + metaData.getRuntimeType() + " not supported");
        }
        return result;
    }


    @Override
    public String toString() {
        return String.format("%s running on [%s]", getDockerImageName(), baseImage);
    }
}
