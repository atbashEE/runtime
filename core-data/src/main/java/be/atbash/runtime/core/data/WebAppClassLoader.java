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
package be.atbash.runtime.core.data;

import be.atbash.runtime.core.data.exception.UnexpectedException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClassLoader used by the sniffers to analyse the deployment. This loader is not used during the processing of user calls.
 */
public class WebAppClassLoader extends ClassLoader {

    private DelegatingURLClassLoader classesClassLoader;
    private DelegatingURLClassLoader descriptorClassLoader;
    private Map<String, DelegatingURLClassLoader> libraryClassLoaders;

    public WebAppClassLoader(File rootDirectory, List<String> libraryFiles, ClassLoader parent) {
        super("WebAppClassLoader", parent);
        defineClassLoader(rootDirectory, parent);
        defineDescriptorLoader(rootDirectory, parent);
        defineLibraryClassLoader(rootDirectory, libraryFiles, parent);
    }

    private void defineLibraryClassLoader(File rootDirectory, List<String> libraryFiles, ClassLoader parent) {
        libraryClassLoaders = new HashMap<>();
        // probably we can create 1 Classloader for all jars in lib directory
        for (String libraryFile : libraryFiles) {
            File jarFile = new File(rootDirectory, "WEB-INF/lib/" + libraryFile);
            libraryClassLoaders.put(libraryFile, defineLibraryClassLoader(jarFile, parent));
        }
    }

    private void defineClassLoader(File rootDirectory, ClassLoader parent) {
        URL classesURL;
        try {
            URI uri = new File(rootDirectory, "WEB-INF/classes/").toURI();
            classesURL = URI.create(uri + "/").toURL();

        } catch (MalformedURLException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }
        classesClassLoader = new DelegatingURLClassLoader(new URL[]{classesURL}, parent);
    }

    private void defineDescriptorLoader(File rootDirectory, ClassLoader parent) {
        URL webInfURL;
        try {
            URI uri = new File(rootDirectory, "WEB-INF/").toURI();
            webInfURL = URI.create(uri + "/").toURL();

        } catch (MalformedURLException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }
        descriptorClassLoader = new DelegatingURLClassLoader(new URL[]{webInfURL}, parent);
    }

    private DelegatingURLClassLoader defineLibraryClassLoader(File jarFile, ClassLoader parent) {
        URL classesURL;
        try {
            classesURL = jarFile.toURI().toURL();

        } catch (MalformedURLException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }
        return new DelegatingURLClassLoader(new URL[]{classesURL}, parent);
    }

    public void close() {
        try {
            classesClassLoader.close();
            descriptorClassLoader.close();
            libraryClassLoaders.values().forEach(cl -> {
                try {
                    cl.close();
                } catch (IOException e) {
                    throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
                }
            });
            descriptorClassLoader.close();
        } catch (IOException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        int index = name.indexOf("!.");
        if (index > 0) {
            // !/ is converted to !. when converting the directory name to class name.
            String jarName = name.substring(0, index);
            String className = name.substring(index + 2);
            return libraryClassLoaders.get(jarName).loadClass(className);
        } else {
            return classesClassLoader.loadClass(name, resolve);
        }
    }

    @Override
    protected URL findResource(String name) {
        int index = name.indexOf("!/");  // Here it is still !/
        if (index > 0) {
            String jarName = name.substring(0, index);
            String resourceName = name.substring(index + 2);
            return libraryClassLoaders.get(jarName).findResource(resourceName);
        } else {
            return descriptorClassLoader.findResource(name);
        }
    }

    private static class DelegatingURLClassLoader extends URLClassLoader {

        private ClassLoader parent;

        public DelegatingURLClassLoader(URL[] urls, ClassLoader parent) {
            super(urls);
            this.parent = parent;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> aClass = null;
            try {

                aClass = super.loadClass(name, resolve);

            } catch (Exception e) {
                // Intentional empty, try also the parent.
            }
            if (aClass == null) {

                aClass = parent.loadClass(name);
            }
            return aClass;
        }
    }

}
