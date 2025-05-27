/*
 *    Copyright 2025 ideal-state
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package team.idealstate.sugar.next.context;

import team.idealstate.sugar.Sugar;
import team.idealstate.sugar.agent.Javaagent;
import team.idealstate.sugar.exception.SugarException;
import team.idealstate.sugar.logging.Log;
import team.idealstate.sugar.maven.resolver.api.Dependency;
import team.idealstate.sugar.maven.resolver.api.DependencyResolver;
import team.idealstate.sugar.maven.resolver.api.MavenResolver;
import team.idealstate.sugar.maven.resolver.api.ResolvedArtifact;
import team.idealstate.sugar.maven.resolver.spi.MavenResolverLoader;
import team.idealstate.sugar.next.context.annotation.feature.EnableSugar;
import team.idealstate.sugar.next.context.exception.ContextException;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

final class SimpleContextLibraryLoader {

    static final String MAVEN_RESOLVER_CONFIG_PATH = "maven/config.xml";
    private static final String MAVEN_RESOLVER_LOADER_NAME = "simple";
    private static final String SUGAR_MAVEN_RESOLVER_DEPENDENCY_ID = "team.idealstate.sugar:sugar-maven-resolver:0.1.0-SNAPSHOT";
    private static volatile MavenResolver mavenResolver;

    @NotNull
    private static MavenResolver getMavenResolver(@NotNull File dataFolder, @NotNull ClassLoader classLoader) {
        if (mavenResolver == null) {
            synchronized (SimpleContextLibraryLoader.class) {
                if (mavenResolver == null) {
                    MavenResolverLoader mavenResolverLoader = MavenResolverLoader.instance(MAVEN_RESOLVER_LOADER_NAME);
                    File configurationFile = new File(dataFolder, MAVEN_RESOLVER_CONFIG_PATH);
                    MavenResolver resolver = mavenResolverLoader.load(configurationFile, classLoader);
                    DependencyResolver dependencyResolver = resolver.getDependencyResolver();
                    Dependency dependency = dependencyResolver.resolve(SUGAR_MAVEN_RESOLVER_DEPENDENCY_ID);
                    List<ResolvedArtifact> artifacts = resolver.resolve(Collections.singletonList(dependency));
                    appendToClassLoaderSearch(classLoader, dependencyResolver.getIdDelimiter(), artifacts);
                    mavenResolver = mavenResolverLoader.load(configurationFile, classLoader);
                }
            }
        }
        return mavenResolver;
    }

    public static void loadDependencies(
            @NotNull ContextHolder holder, @NotNull File dataFolder, @NotNull ClassLoader classLoader) {
        Validation.notNull(holder, "Context holder must not be null.");
        Validation.notNull(dataFolder, "Data folder must not be null.");
        Validation.notNull(classLoader, "Class loader must not be null.");
        if (!(classLoader instanceof URLClassLoader)) {
            return;
        }
        Class<? extends ContextHolder> holderType = holder.getClass();
        if (!holderType.isAnnotationPresent(EnableSugar.class)) {
            return;
        }
        CodeSource codeSource = holderType.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            return;
        }
        URL location = codeSource.getLocation();
        if (location == null) {
            return;
        }
        File file;
        try {
            file = new File(location.toURI());
        } catch (URISyntaxException e) {
            throw new ContextException(e);
        }
        Set<Dependency> dependencies = new LinkedHashSet<>(128);
        MavenResolver mavenResolver = null;
        DependencyResolver dependencyResolver = null;
        try (JarFile jar = new JarFile(file)) {
            Manifest manifest = jar.getManifest();
            if (manifest == null) {
                return;
            }
            Attributes attributes = manifest.getMainAttributes();
            if (attributes.isEmpty()) {
                return;
            }
            Sugar sugar = Sugar.load(jar, false);
            if (sugar == null || !sugar.isEnabled()) {
                return;
            }
            String pomPath = String.format("META-INF/maven/%s/%s/pom.xml", sugar.getGroupId(), sugar.getArtifactId());
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entry.isDirectory() || !entryName.equals(pomPath)) {
                    continue;
                }
                if (mavenResolver == null) {
                    mavenResolver = getMavenResolver(dataFolder, classLoader);
                }
                if (dependencyResolver == null) {
                    dependencyResolver = mavenResolver.getDependencyResolver();
                }
                try (InputStream input = jar.getInputStream(entry)) {
                    List<? extends Dependency> resolved = mavenResolver.getDependencyResolver().resolvePom(input);
                    if (resolved.isEmpty()) {
                        continue;
                    }
                    dependencies.addAll(resolved);
                }
            }
        } catch (IOException e) {
            throw new SugarException(e);
        }
        if (dependencies.isEmpty()) {
            return;
        }
        List<ResolvedArtifact> artifacts = mavenResolver.resolve(new ArrayList<>(dependencies));
        appendToClassLoaderSearch(classLoader, dependencyResolver.getIdDelimiter(), artifacts);
    }

    private static void appendToClassLoaderSearch(@NotNull ClassLoader classLoader, @NotNull String dependencyIdDelimiter, @NotNull List<ResolvedArtifact> artifacts) {
        Set<String> loaded = new HashSet<>(artifacts.size());
        for (ResolvedArtifact artifact : artifacts) {
            File artifactFile = artifact.getFile();
            if (artifactFile.isDirectory() || !artifactFile.getName().endsWith(".jar")) {
                continue;
            }
            String path = artifactFile.toPath().normalize().toString();
            if (!loaded.add(path)) {
                continue;
            }
            String id = new StringJoiner(dependencyIdDelimiter)
                    .add(artifact.getGroupId())
                    .add(artifact.getArtifactId())
                    .add(artifact.getExtension())
                    .add(artifact.getClassifier())
                    .add(artifact.getVersion())
                    .toString();
            try {
                appendToClassLoaderSearch(classLoader, id, artifactFile);
            } catch (MalformedURLException | InvocationTargetException | IllegalAccessException e) {
                throw new ContextException(e);
            }
        }
    }

    private static final Method ADD_URL;

    static {
        try {
            ADD_URL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            ADD_URL.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static void appendToClassLoaderSearch(ClassLoader classLoader, String id, File file)
            throws MalformedURLException, InvocationTargetException, IllegalAccessException {
        if (file != null && file.exists()) {
            if (classLoader instanceof URLClassLoader) {
                ADD_URL.invoke(classLoader, file.toURI().toURL());
                Log.info(() -> String.format("Append to context classpath: '%s'", id));
            } else if (Javaagent.isLoaded()) {
                Javaagent.appendToSystemClassLoaderSearch(Collections.singletonMap(id, file));
            } else {
                throw new ContextException(String.format("Class loader '%s' is not supported.", classLoader));
            }
        }
    }
}
