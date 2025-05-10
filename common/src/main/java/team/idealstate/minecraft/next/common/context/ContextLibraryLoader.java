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

package team.idealstate.minecraft.next.common.context;

import static team.idealstate.minecraft.next.common.function.Functional.functional;
import static team.idealstate.minecraft.next.common.function.Functional.lazy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.objectweb.asm.ClassReader;
import team.idealstate.minecraft.next.common.context.exception.ContextException;
import team.idealstate.minecraft.next.common.function.Lazy;
import team.idealstate.minecraft.next.common.io.IOUtils;
import team.idealstate.minecraft.next.common.logging.Log;
import team.idealstate.minecraft.next.common.maven.MavenResolver;
import team.idealstate.minecraft.next.common.maven.PomXmlResolver;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

public class ContextLibraryLoader implements ClassFileTransformer {

    private static final Class<?> SUPER_CLASS = ContextHolder.class;
    private static final String SUPER_CLASS_NAME = ContextHolder.class.getName();
    private final Set<String> resolved = new CopyOnWriteArraySet<>();
    private final Lazy<MavenResolver> mavenResolver = lazy(MavenResolver::new);
    private final Instrumentation instrumentation;

    public ContextLibraryLoader(@NotNull Instrumentation instrumentation) {
        Validation.notNull(instrumentation, "instrumentation must not be null.");
        this.instrumentation = instrumentation;
        try {
            loadDependencies(
                    new File(
                            SUPER_CLASS
                                    .getProtectionDomain()
                                    .getCodeSource()
                                    .getLocation()
                                    .toURI()),
                    true);
        } catch (URISyntaxException e) {
            throw new ContextException(e);
        }
    }

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] buffer)
            throws IllegalClassFormatException {
        if (loader == null || classBeingRedefined != null) {
            return null;
        }
        CodeSource codeSource = protectionDomain.getCodeSource();
        if (codeSource == null) {
            return null;
        }
        URL location = codeSource.getLocation();
        if (location == null) {
            return null;
        }
        File file;
        try {
            file = new File(location.toURI());
            if (isResolved(file)) {
                return null;
            }
            String[] interfaces = new ClassReader(buffer).getInterfaces();
            if (interfaces.length == 0) {
                return null;
            }
            if (Arrays.stream(interfaces).noneMatch(SUPER_CLASS_NAME::equals)) {
                return null;
            }
            if (!SUPER_CLASS.equals(Class.forName(SUPER_CLASS.getName(), false, loader))) {
                return null;
            }
        } catch (ClassNotFoundException | URISyntaxException e) {
            return null;
        }
        loadDependencies(file, false);
        return null;
    }

    private boolean isResolved(@NotNull File file) {
        return !resolved.add(file.getAbsolutePath());
    }

    private void loadDependencies(@NotNull File file, boolean verify) {
        if (verify) {
            if (isResolved(file)) {
                return;
            }
        }
        ByteArrayInputStream inputStream;
        try {
            inputStream =
                    functional(new JarFile(file))
                            .use(
                                    ByteArrayInputStream.class,
                                    jar -> {
                                        Enumeration<JarEntry> entries = jar.entries();
                                        while (entries.hasMoreElements()) {
                                            JarEntry entry = entries.nextElement();
                                            String entryName = entry.getName();
                                            if (entry.isDirectory() || !entryName.startsWith("META-INF/maven/") || !entryName.endsWith("pom.xml")) {
                                                continue;
                                            }
                                            return new ByteArrayInputStream(
                                                    IOUtils.readAllBytes(
                                                            jar.getInputStream(entry)));
                                        }
                                        return null;
                                    });
        } catch (IOException e) {
            throw new ContextException(e);
        }
        if (inputStream == null) {
            return;
        }
        List<Dependency> dependencies = PomXmlResolver.resolve(inputStream);
        if (dependencies.isEmpty()) {
            return;
        }
        MavenResolver mavenResolver = this.mavenResolver.get();
        List<Artifact> artifacts = mavenResolver.resolve(dependencies);
        if (artifacts.isEmpty()) {
            return;
        }
        for (Artifact artifact : artifacts) {
            File artifactFile = artifact.getFile();
            if (!artifactFile.exists()
                    || artifactFile.isDirectory()
                    || !artifactFile.getName().endsWith(".jar")) {
                continue;
            }
            try {
                Log.info(String.format("loading dependency %s", artifactFile.getAbsolutePath()));
                instrumentation.appendToSystemClassLoaderSearch(new JarFile(artifactFile));
            } catch (IOException e) {
                throw new ContextException(e);
            }
        }
    }
}
