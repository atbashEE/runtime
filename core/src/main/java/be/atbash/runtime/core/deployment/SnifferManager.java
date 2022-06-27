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
package be.atbash.runtime.core.deployment;

import be.atbash.runtime.core.data.WebAppClassLoader;
import be.atbash.runtime.core.data.deployment.ArchiveContent;
import be.atbash.runtime.core.data.module.sniffer.Sniffer;
import be.atbash.util.exception.AtbashUnexpectedException;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class SnifferManager {

    private static final SnifferManager INSTANCE = new SnifferManager();

    private final List<Class<? extends Sniffer>> sniffers = new ArrayList<>();

    private SnifferManager() {
    }

    public void registerSniffer(Class<? extends Sniffer> sniffer) {
        if (sniffer != null && !sniffers.contains(sniffer)) {
            sniffers.add(sniffer);
        }
    }

    public SpecificationChecker startSpecificationCheck(ArchiveContent archiveContent, WebAppClassLoader classLoader) {
        return new SpecificationChecker(archiveContent, classLoader, createSnifferInstances(true, null));
    }


    public List<Sniffer> retrieveSniffers(List<String> snifferNames) {
        return createSnifferInstances(false, snifferNames);

    }

    private List<Sniffer> createSnifferInstances(boolean allSniffers, List<String> snifferNames) {
        Predicate<Class<? extends Sniffer>> classPredicate;
        if (allSniffers) {
            classPredicate = c -> true;
        } else {
            classPredicate = c -> snifferNames.contains(c.getSimpleName());
        }
        return sniffers.stream()
                .filter(classPredicate)
                .map(this::newSnifferInstance)
                .collect(Collectors.toList());
    }

    private Sniffer newSnifferInstance(Class<? extends Sniffer> snifferClass) {
        try {
            return snifferClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new AtbashUnexpectedException(e);
        }
    }

    public static SnifferManager getInstance() {
        return INSTANCE;
    }
}
