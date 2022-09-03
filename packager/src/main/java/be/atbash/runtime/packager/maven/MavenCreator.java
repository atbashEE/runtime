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
package be.atbash.runtime.packager.maven;

import be.atbash.runtime.core.data.exception.UnexpectedException;
import be.atbash.runtime.core.data.version.VersionReader;
import be.atbash.runtime.packager.files.FileCreator;
import be.atbash.runtime.packager.model.Module;
import be.atbash.runtime.packager.model.PackagerOptions;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 */
public final class MavenCreator {

    private final String version;

    private final FileCreator fileCreator;

    private final MavenHelper mavenHelper;

    private final PackagerOptions options;

    private MavenCreator(PackagerOptions options) {
        this.options = options;
        fileCreator = new FileCreator();
        mavenHelper = new MavenHelper();

        VersionReader versionReader = new VersionReader("packager");
        version = versionReader.getReleaseVersion();

    }

    private void create() {

        Model pomFile = createMavenModel(options);

        writePOMFile(pomFile, options.getTargetDirectory());

    }

    private void writePOMFile(Model pomFile, File directory) {
        String content;
        MavenXpp3Writer pomWriter = new MavenXpp3Writer();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pomWriter.write(out, pomFile);
            out.close();
            content = out.toString();
        } catch (IOException e) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }

        fileCreator.writeContents(directory, "pom.xml", content);
    }

    private Model createMavenModel(PackagerOptions model) {

        Model pomFile = new Model();
        pomFile.setModelVersion("4.0.0");

        pomFile.setGroupId("be.atbash.packager");
        pomFile.setArtifactId(model.getArtifactId());
        pomFile.setVersion(version);

        pomFile.setPackaging("jar");

        // As the first dependency to force the correct version.
        mavenHelper.addDependency(pomFile, "org.slf4j", "slf4j-api", "2.0.0-alpha6");

        mavenHelper.addDependency(pomFile, "be.atbash.runtime", "runtime-main", version, List.of("*:*"));
        // The next one is required due to the exclusion of *:*  But it also excludes all modules from runtime-main itself (the main packaging)
        mavenHelper.addDependency(pomFile, "be.atbash.runtime", "runtime-common", version);

        // Based on the specified modules on the command line
        addDependencies(pomFile, model);

        addJavaSEVersionProperties(pomFile);

        Build build = defineBuildElement(model.getArtifactId());
        pomFile.setBuild(build);

        return pomFile;
    }

    private Build defineBuildElement(String artifactId) {
        Build build = new Build();
        build.setFinalName(artifactId);

        Plugin plugin = defineJarPluginWithArchive();
        build.addPlugin(plugin);

        plugin = defineAssemblyPlugin();
        build.addPlugin(plugin);

        return build;
    }

    private Plugin defineJarPluginWithArchive() {
        Plugin result = new Plugin();
        result.setGroupId("org.apache.maven.plugins");
        result.setArtifactId("maven-jar-plugin");
        result.setVersion("2.5");

        List<PluginExecution> executions = new ArrayList<>();
        executions.add(executionWithGoal("manifest", null, "jar"));
        result.setExecutions(executions);

        result.setConfiguration(defineJarPluginConfiguration());

        return result;
    }

    private Xpp3Dom defineJarPluginConfiguration() {
        Xpp3Dom configuration = new Xpp3Dom("configuration");
        Xpp3Dom archive = new Xpp3Dom("archive");
        configuration.addChild(archive);

        Xpp3Dom manifest = new Xpp3Dom("manifest");
        manifest.addChild(defineElement("addClasspath", "true"));
        manifest.addChild(defineElement("classpathPrefix", "lib/"));
        manifest.addChild(defineElement("classpathLayoutType", "custom"));
        manifest.addChild(defineElement("customClasspathLayout", "$${artifact.groupId}.$${artifact.artifactId}.$${artifact.extension}"));
        archive.addChild(manifest);

        Xpp3Dom manifestEntries = new Xpp3Dom("manifestEntries");
        manifestEntries.addChild(defineElement("Main-Class", "be.atbash.runtime.RuntimeMain"));
        manifestEntries.addChild(defineElement("Release-Version", version));
        manifestEntries.addChild(defineElement("buildTime", createTimeStamp()));
        archive.addChild(manifestEntries);

        return configuration;
    }

    private String createTimeStamp() {
        SimpleDateFormat fmt = new SimpleDateFormat(" yyyy-MM-dd'T'HH:mm:ss'Z'");
        return fmt.format(new Date());
    }

    private Xpp3Dom defineElement(String name, String value) {
        Xpp3Dom element = new Xpp3Dom(name);
        element.setValue(value);
        return element;
    }

    private PluginExecution executionWithGoal(String id, String phase, String goal) {
        PluginExecution result = new PluginExecution();
        result.setId(id);
        if (phase != null) {
            result.setPhase(phase);
        }
        result.addGoal(goal);
        return result;
    }

    private Plugin defineAssemblyPlugin() {
        Plugin result = new Plugin();

        result.setGroupId("org.apache.maven.plugins");
        result.setArtifactId("maven-assembly-plugin");
        result.setVersion("3.1.0");

        List<PluginExecution> executions = new ArrayList<>();
        executions.add(executionWithGoal("make-assembly", "package", "single"));
        result.setExecutions(executions);

        result.setConfiguration(defineAssemblyPluginConfiguration());

        return result;
    }

    private Xpp3Dom defineAssemblyPluginConfiguration() {
        Xpp3Dom configuration = new Xpp3Dom("configuration");
        Xpp3Dom archive = new Xpp3Dom("archive");
        configuration.addChild(archive);

        Xpp3Dom manifest = new Xpp3Dom("manifest");
        manifest.addChild(defineElement("mainClass", "be.atbash.runtime.RuntimeMain"));
        archive.addChild(manifest);

        Xpp3Dom descriptors = new Xpp3Dom("descriptors");
        descriptors.addChild(defineElement("descriptor", "src/assembly/assembly.xml"));

        configuration.addChild(descriptors);

        configuration.addChild(defineElement("appendAssemblyId", "false"));

        return configuration;
    }


    private void addDependencies(Model pomFile, PackagerOptions model) {
        model.getRequestedModules().forEach(m -> addDependency(pomFile, m));
    }

    private void addDependency(Model pomFile, Module module) {
        mavenHelper.addDependency(pomFile, "be.atbash.runtime", module.getArtifactId(), version);
    }

    private void addJavaSEVersionProperties(Model pomFile) {

        pomFile.addProperty("maven.compiler.source", "11");  // Does not make sense to set to something else since there is no source code in project
        pomFile.addProperty("maven.compiler.target", "11");
    }

    public static void createMavenFiles(PackagerOptions options) {
        new MavenCreator(options).create();
    }
}
