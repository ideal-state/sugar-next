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

package team.idealstate.minecraft.next.common.bytecode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import team.idealstate.minecraft.next.common.bytecode.api.member.JavaClass;
import team.idealstate.minecraft.next.common.bytecode.exception.BytecodeParsingException;
import team.idealstate.minecraft.next.common.function.Functional;
import team.idealstate.minecraft.next.common.io.InputUtils;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

public final class JavaCache extends ConcurrentHashMap<String, JavaClass> {
    private static final long serialVersionUID = 7815213748903816664L;

    private final File jarFile;

    public JavaCache() {
        this.jarFile = null;
    }

    public JavaCache(int initialCapacity) {
        this(null, initialCapacity);
    }

    public JavaCache(Map<? extends String, ? extends JavaClass> m) {
        this(null, m);
    }

    public JavaCache(int initialCapacity, float loadFactor) {
        this(null, initialCapacity, loadFactor);
    }

    public JavaCache(int initialCapacity, float loadFactor, int concurrencyLevel) {
        this(null, initialCapacity, loadFactor, concurrencyLevel);
    }

    public JavaCache(File jarFile) {
        this.jarFile = jarFile;
    }

    public JavaCache(File jarFile, int initialCapacity) {
        super(initialCapacity);
        this.jarFile = jarFile;
    }

    public JavaCache(File jarFile, Map<? extends String, ? extends JavaClass> m) {
        super(m);
        this.jarFile = jarFile;
    }

    public JavaCache(File jarFile, int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        this.jarFile = jarFile;
    }

    public JavaCache(File jarFile, int initialCapacity, float loadFactor, int concurrencyLevel) {
        super(initialCapacity, loadFactor, concurrencyLevel);
        this.jarFile = jarFile;
    }

    @NotNull JavaClass forName(@NotNull String className) {
        return forName(className, null);
    }

    @NotNull JavaClass forName(@NotNull String className, ClassLoader classLoader) {
        Validation.notNullOrBlank(className, "className must not be null or blank.");
        String classpath = className.replace('\\', '/');
        if (classpath.endsWith(".class")) {
            className = classpath.substring(0, className.length() - 6);
        } else {
            classpath += ".class";
        }
        className = className.replace('/', '.');
        JavaClass javaClass = get(className);
        if (javaClass != null) {
            return javaClass;
        }
        if (jarFile != null) {
            try {
                AtomicReference<Object> classFile = new AtomicReference<>();
                String finalClasspath = classpath;
                Functional.functional(new JarFile(jarFile))
                        .use(
                                jar -> {
                                    JarEntry entry = jar.getJarEntry(finalClasspath);
                                    if (entry == null) {
                                        return;
                                    }
                                    Functional.functional(jar.getInputStream(entry))
                                            .use(
                                                    input -> {
                                                        classFile.set(InputUtils.readStream(input));
                                                    });
                                });
                javaClass =
                        InternalJavaClass.newInstance(className, (byte[]) classFile.get(), this);
            } catch (IOException e) {
                throw new BytecodeParsingException(e);
            }
        }
        if (javaClass == null) {
            if (classLoader == null) {
                classLoader = getClass().getClassLoader();
            }
            try (InputStream inputStream = classLoader.getResourceAsStream("/" + classpath)) {
                if (inputStream == null) {
                    throw new BytecodeParsingException("cannot find class " + className);
                }
                javaClass = InternalJavaClass.newInstance(className, inputStream, this);
            } catch (IOException e) {
                throw new BytecodeParsingException(e);
            }
        }
        return javaClass;
    }
}
