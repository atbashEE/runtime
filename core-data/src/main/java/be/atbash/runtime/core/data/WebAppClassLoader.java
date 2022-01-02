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

/**
 * ClassLoader used by the sniffers to analyse the deployment. This loader is not used during the processing of user calls.
 */
public class WebAppClassLoader extends ClassLoader {

    private DelegatingURLClassLoader classesClassLoader;
    private DelegatingURLClassLoader descriptorClassLoader;
    //private URLClassLoader libClassLoader;  TODO Scan also libs for Servlets, JAX-RS endpoints, ... ?

    public WebAppClassLoader(File rootDirectory, ClassLoader parent) {
        super("WebAppClassLoader", parent);
        defineClassLoader(rootDirectory, parent);
        defineDescriptorLoader(rootDirectory, parent);
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

    public void close() {
        try {
            classesClassLoader.close();
        } catch (IOException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return classesClassLoader.loadClass(name, resolve);
    }

    @Override
    protected URL findResource(String name) {
        return descriptorClassLoader.findResource(name);
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
