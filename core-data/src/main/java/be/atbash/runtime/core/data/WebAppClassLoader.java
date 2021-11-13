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
package be.atbash.runtime.core.data;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;

public class WebAppClassLoader extends ClassLoader {

    private DelegatingURLClassLoader classesClassLoader;
    private URLClassLoader libClassLoader;

    public WebAppClassLoader(File rootDirectory, ClassLoader parent) {
        super("WebAppClassLoader", parent);
        URL url = null;
        try {
            URI uri = new File(rootDirectory, "WEB-INF/classes/").toURI();
            url = uri.toURL();
            url = URI.create(uri.toString() + "/").toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        classesClassLoader = new DelegatingURLClassLoader(new URL[]{url}, parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return classesClassLoader.loadClass(name, resolve);
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
                ///
            }
            if (aClass == null) {

                aClass = parent.loadClass(name);
            }
            return aClass;
        }
    }

}
