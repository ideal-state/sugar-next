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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import team.idealstate.sugar.Sugar;
import team.idealstate.sugar.exception.SugarException;
import team.idealstate.sugar.internal.org.eclipse.aether.graph.Dependency;
import team.idealstate.sugar.logging.Log;
import team.idealstate.sugar.maven.MavenResolver;
import team.idealstate.sugar.maven.PomXmlResolver;
import team.idealstate.sugar.next.context.annotation.feature.EnableSugar;
import team.idealstate.sugar.next.context.exception.ContextException;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;

final class SimpleContextLibraryLoader {

    private static volatile MavenResolver mavenResolver;

    @NotNull
    private static MavenResolver getMavenResolver(@NotNull File dataFolder) {
        if (mavenResolver == null) {
            synchronized (SimpleContextLibraryLoader.class) {
                if (mavenResolver == null) {
                    mavenResolver = new MavenResolver(dataFolder);
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
                try (InputStream input = jar.getInputStream(entry)) {
                    List<Dependency> resolve = PomXmlResolver.resolve(input);
                    if (resolve.isEmpty()) {
                        continue;
                    }
                    dependencies.addAll(resolve);
                }
            }
        } catch (IOException e) {
            throw new SugarException(e);
        }
        if (dependencies.isEmpty()) {
            return;
        }
        MavenResolver mavenResolver = getMavenResolver(dataFolder);
        Map<String, File> artifacts =
                MavenResolver.notMissingOrEx(mavenResolver.resolve(new ArrayList<>(dependencies)));
        if (artifacts.isEmpty()) {
            return;
        }
        Set<String> loaded = new HashSet<>(artifacts.size());
        for (Map.Entry<String, File> entry : artifacts.entrySet()) {
            File artifact = entry.getValue();
            if (artifact.isDirectory() || !artifact.getName().endsWith(".jar")) {
                continue;
            }
            String path = artifact.toPath().normalize().toString();
            if (!loaded.add(path)) {
                continue;
            }
            String id = entry.getKey();
            try {
                if (appendToURLClassLoaderSearch((URLClassLoader) classLoader, artifact)) {
                    Log.info(() -> String.format("Append to context classpath: '%s'", id));
                }
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

    private static boolean appendToURLClassLoaderSearch(URLClassLoader urlClassLoader, File file)
            throws MalformedURLException, InvocationTargetException, IllegalAccessException {
        if (file != null && file.exists()) {
            ADD_URL.invoke(urlClassLoader, file.toURI().toURL());
            return true;
        }
        return false;
    }
}
