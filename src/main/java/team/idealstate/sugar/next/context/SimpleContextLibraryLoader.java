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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collection;
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

final class SimpleContextLibraryLoader {

    static final String MAVEN_RESOLVER_CONFIG_PATH = "maven/config.xml";
    private static final String MAVEN_RESOLVER_LOADER_NAME = "simple";
    private static final String SUGAR_MAVEN_RESOLVER_DEPENDENCY_ID = "team.idealstate.sugar:sugar-maven-resolver:0.1.0";
    private static volatile MavenResolver mavenResolver;

    @NotNull
    private static MavenResolver getMavenResolver(@NotNull File dataFolder, @NotNull ClassLoader classLoader) {
        if (mavenResolver == null) {
            synchronized (SimpleContextLibraryLoader.class) {
                if (mavenResolver == null) {
                    MavenResolverLoader mavenResolverLoader =
                            MavenResolverLoader.instance(MAVEN_RESOLVER_LOADER_NAME, classLoader);
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
                    List<? extends Dependency> resolved =
                            mavenResolver.getDependencyResolver().resolvePom(input);
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

    private static void appendToClassLoaderSearch(
            @NotNull ClassLoader classLoader,
            @NotNull String dependencyIdDelimiter,
            @NotNull List<ResolvedArtifact> artifacts) {
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
            } catch (MalformedURLException | ReflectiveOperationException e) {
                throw new ContextException(e);
            }
        }
    }

    private static void appendToClassLoaderSearch(ClassLoader classLoader, String id, File file)
            throws MalformedURLException, ReflectiveOperationException {
        if (file != null && file.exists()) {
            if (classLoader instanceof URLClassLoader) {
                addURL((URLClassLoader) classLoader, file.toURI().toURL());
                Log.info(() -> String.format("Append to context classpath: '%s'", id));
            } else if (Javaagent.isLoaded()) {
                Javaagent.appendToSystemClassLoaderSearch(Collections.singletonMap(id, file));
            } else {
                throw new ContextException(String.format("Class loader '%s' is not supported.", classLoader));
            }
        }
    }

    private static volatile Object unsafe = null;
    private static volatile Method objectFieldOffset = null;
    private static volatile Method getReference = null;
    private static volatile Method getBoolean = null;
    private static volatile Long ucpOffset = null;
    private static volatile Long closedOffset = null;
    private static volatile Long unopenedUrlsOffset = null;
    private static volatile Long pathOffset = null;
    private static volatile Method addUrl = null;

    private static void addURL(URLClassLoader ucl, URL url) throws ReflectiveOperationException {
        Class<?> unsafeClass;
        try {
            Class.forName("jdk.internal.misc.Unsafe");
            unsafeClass = Class.forName("sun.misc.Unsafe");
        } catch (ClassNotFoundException e) {
            if (addUrl == null) {
                addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                addUrl.setAccessible(true);
            }
            addUrl.invoke(ucl, url);
            return;
        }
        if (unsafe == null) {
            Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = theUnsafe.get(null);
        }
        if (objectFieldOffset == null) {
            objectFieldOffset = unsafeClass.getDeclaredMethod("objectFieldOffset", Field.class);
        }
        if (getReference == null) {
            getReference = unsafeClass.getDeclaredMethod("getObject", Object.class, long.class);
        }
        if (getBoolean == null) {
            getBoolean = unsafeClass.getDeclaredMethod("getBoolean", Object.class, long.class);
        }
        if (ucpOffset == null) {
            ucpOffset = (Long) objectFieldOffset.invoke(unsafe, URLClassLoader.class.getDeclaredField("ucp"));
        }
        addURL(getReference.invoke(unsafe, ucl, ucpOffset), url);
    }

    @SuppressWarnings({"unchecked", "SynchronizationOnLocalVariableOrMethodParameter"})
    private static void addURL(Object ucp, URL url)
            throws InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        Class<?> ucpClass = ucp.getClass();
        if (closedOffset == null) {
            closedOffset = (Long) objectFieldOffset.invoke(unsafe, ucpClass.getDeclaredField("closed"));
        }
        if ((boolean) getBoolean.invoke(unsafe, ucp, closedOffset)) {
            return;
        }
        if (unopenedUrlsOffset == null) {
            unopenedUrlsOffset = (Long) objectFieldOffset.invoke(unsafe, ucpClass.getDeclaredField("unopenedUrls"));
        }
        Collection<URL> unopenedUrls = (Collection<URL>) getReference.invoke(unsafe, ucp, unopenedUrlsOffset);
        if ((boolean) getBoolean.invoke(unsafe, ucp, closedOffset)) {
            return;
        }
        synchronized (unopenedUrls) {
            if (pathOffset == null) {
                pathOffset = (Long) objectFieldOffset.invoke(unsafe, ucpClass.getDeclaredField("path"));
            }
            Collection<URL> path = (Collection<URL>) getReference.invoke(unsafe, ucp, pathOffset);
            if (!path.contains(url)) {
                unopenedUrls.add(url);
                path.add(url);
            }
        }
    }
}
