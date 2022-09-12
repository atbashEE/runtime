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

package be.atbash.runtime.packager.files;

import be.atbash.runtime.core.data.exception.UnexpectedException;
import be.atbash.runtime.core.data.util.ResourceReader;

import java.io.IOException;
import java.util.Map;

import static be.atbash.util.resource.ResourceUtil.CLASSPATH_PREFIX;

public class TemplateEngine {

    public void processTemplateFile(String directory, String templateFileName, String fileName,
                                    Map<String, String> variables) {
        String templateContent = readTemplateFile(CLASSPATH_PREFIX + "templates/" + templateFileName);
        String content = resolveVariables(templateContent, variables);

        FileCreator fileCreator = new FileCreator();

        fileCreator.writeContents(directory, fileName, content);

    }

    private String resolveVariables(String content, Map<String, String> variables) {
        String result = content;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace(definePlaceholder(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private String definePlaceholder(String key) {
        return String.format("%%%s%%", key);
    }

    private String readTemplateFile(String location) {
        String content;
        try {
            content = ResourceReader.readResource(location);
        } catch (IOException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }
        return content;
    }
}
