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

import java.io.Closeable;
import java.io.File;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.objectweb.asm.ClassReader;
import team.idealstate.minecraft.next.common.banner.Banner;
import team.idealstate.minecraft.next.common.bundled.Bundled;
import team.idealstate.minecraft.next.common.bytecode.Java;
import team.idealstate.minecraft.next.common.bytecode.JavaCache;
import team.idealstate.minecraft.next.common.bytecode.api.member.JavaClass;
import team.idealstate.minecraft.next.common.bytecode.api.struct.JavaAnnotation;
import team.idealstate.minecraft.next.common.context.annotation.NextCommand;
import team.idealstate.minecraft.next.common.context.annotation.NextConfiguration;
import team.idealstate.minecraft.next.common.context.annotation.NextLazy;
import team.idealstate.minecraft.next.common.context.annotation.NextPlaceholder;
import team.idealstate.minecraft.next.common.context.exception.ContextException;
import team.idealstate.minecraft.next.common.context.factory.NextCommandInstanceFactory;
import team.idealstate.minecraft.next.common.context.factory.NextPlaceholderInstanceFactory;
import team.idealstate.minecraft.next.common.function.Functional;
import team.idealstate.minecraft.next.common.function.Lazy;
import team.idealstate.minecraft.next.common.function.closure.Function;
import team.idealstate.minecraft.next.common.logging.Log;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;
import team.idealstate.minecraft.next.common.validate.annotation.Nullable;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class SimpleContext implements Context {
    private static final int STATUS_ERROR = -1;
    private static final int STATUS_DESTROYED = 0;
    private static final int STATUS_INITIALIZED = 1;
    private static final int STATUS_LOADED = 2;
    private static final int STATUS_ENABLED = 3;
    private static final int STATUS_DISABLED = 4;

    @NonNull private final ContextHolder contextHolder;
    @NonNull private final Lifecycle lifecycle;
    private volatile int status = STATUS_DESTROYED;
    private final Lock lock = new ReentrantLock();
    private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
    private static final long TIMEOUT = 100L;

    private <R> R mustDependOn(
            int depend,
            boolean safe,
            boolean allowTimeout,
            boolean next,
            R def,
            Function<SimpleContext, R> function) {
        int current = this.status;
        try {
            Validation.vote(current <= STATUS_ERROR, "error status " + current + ".");
            Validation.vote(current == depend, "status must be " + depend + ".");
        } catch (Throwable e) {
            throw new ContextException(e);
        }
        if (safe) {
            try {
                if (!lock.tryLock(TIMEOUT, TIMEOUT_UNIT)) {
                    if (!allowTimeout) {
                        this.status = STATUS_ERROR;
                    }
                    return def;
                }
            } catch (InterruptedException e) {
                this.status = STATUS_ERROR;
                Thread.currentThread().interrupt();
                return def;
            } catch (Throwable e) {
                this.status = STATUS_ERROR;
                throw new ContextException(e);
            }
        }
        try {
            R result = function.call(this);
            if (next) {
                if (current + 1 > STATUS_DISABLED) {
                    this.status = STATUS_DESTROYED;
                } else {
                    this.status = current + 1;
                }
            }
            return result;
        } catch (Throwable e) {
            this.status = STATUS_ERROR;
            throw new ContextException(e);
        } finally {
            if (safe) {
                lock.unlock();
            }
        }
    }

    @NotNull @Override
    public String getName() {
        return Validation.requireNotNullOrBlank(
                contextHolder.getName(), "name must not be null or blank.");
    }

    @NotNull @Override
    public String getVersion() {
        return Validation.requireNotNullOrBlank(
                contextHolder.getVersion(), "version must not be null or blank.");
    }

    @NotNull @Override
    public File getDataFolder() {
        return Validation.requireNotNull(
                contextHolder.getDataFolder(), "data folder must not be null.");
    }

    @Override
    public boolean isActive() {
        return status == STATUS_ENABLED;
    }

    @Override
    public void initialize() {
        mustDependOn(
                STATUS_DESTROYED,
                true,
                false,
                true,
                null,
                it -> {
                    it.lifecycle.onInitialize(this);
                    it.doInitialize();
                    it.lifecycle.onInitialized(this);
                    return null;
                });
    }

    @Override
    public void load() {
        mustDependOn(
                STATUS_INITIALIZED,
                true,
                false,
                true,
                null,
                it -> {
                    it.lifecycle.onLoad(this);
                    it.doLoad();
                    it.lifecycle.onLoaded(this);
                    return null;
                });
    }

    @Override
    public void enable() {
        mustDependOn(
                STATUS_LOADED,
                true,
                false,
                true,
                null,
                it -> {
                    it.lifecycle.onEnable(this);
                    it.doEnable();
                    it.lifecycle.onEnabled(this);
                    return null;
                });
    }

    @Override
    public void disable() {
        mustDependOn(
                STATUS_ENABLED,
                true,
                false,
                true,
                null,
                it -> {
                    it.lifecycle.onDisable(this);
                    it.doDisable();
                    it.lifecycle.onDisabled(this);
                    return null;
                });
    }

    @Override
    public void destroy() {
        mustDependOn(
                STATUS_DISABLED,
                true,
                false,
                true,
                null,
                it -> {
                    it.lifecycle.onDestroy(this);
                    it.doDestroy();
                    it.lifecycle.onDestroyed(this);
                    return null;
                });
    }

    private final Map<Class<? extends Annotation>, InstanceFactory<?, ?>> instanceFactories =
            new ConcurrentHashMap<>();

    @Nullable @Override
    public <M extends Annotation> InstanceFactory<M, ?> instanceFactoryBy(
            @NotNull Class<M> metadataClass) {
        return instanceFactoryBy(metadataClass, Object.class);
    }

    @Nullable @Override
    @SuppressWarnings({"unchecked"})
    public <M extends Annotation, T> InstanceFactory<M, T> instanceFactoryBy(
            @NotNull Class<M> metadataClass, @NotNull Class<T> instanceClass) {
        Validation.notNull(metadataClass, "metadataClass must not be null.");
        Validation.notNull(instanceClass, "instanceClass must not be null.");
        InstanceFactory<?, ?> instanceFactory = instanceFactories.get(metadataClass);
        if (instanceFactory != null) {
            Validation.vote(
                    metadataClass.isAssignableFrom(instanceFactory.getMetadataClass()),
                    "metadataClass must be assignable to instanceFactory.getMetadataClass()");
            if (!instanceClass.isAssignableFrom(instanceFactory.getInstanceClass())) {
                return null;
            }
        }
        return (InstanceFactory<M, T>) instanceFactory;
    }

    @Override
    public <M extends Annotation> void setInstanceFactory(
            @NotNull Class<M> metadataClass, @NotNull InstanceFactory<M, ?> componentFactory) {
        Validation.notNull(metadataClass, "metadataClass must not be null");
        Validation.notNull(componentFactory, "componentFactory must not be null");
        Class<M> factoryMetadataClass = componentFactory.getMetadataClass();
        Validation.notNull(factoryMetadataClass, "factoryMetadataClass must not be null");
        Validation.vote(
                factoryMetadataClass.isAssignableFrom(metadataClass),
                "metadataClass must be assignable to factoryMetadataClass");
        instanceFactories.put(metadataClass, componentFactory);
    }

    private final Map<Class<? extends Annotation>, Deque<Class<?>>> markedClasses =
            new ConcurrentHashMap<>();
    private final Map<ComponentKey, SimpleComponent<?, ?>> components = new ConcurrentHashMap<>();
    private final Set<ComponentKey> inProgress = new CopyOnWriteArraySet<>();

    @Nullable @Override
    public <M extends Annotation> Component<M, ?> componentBy(@NotNull Class<M> metadataClass) {
        return componentBy(metadataClass, Object.class);
    }

    @Nullable @Override
    @SuppressWarnings({"unchecked", "DuplicatedCode"})
    public <M extends Annotation, T> Component<M, T> componentBy(
            @NotNull Class<M> metadataClass, @NotNull Class<T> instanceClass) {
        Validation.notNull(metadataClass, "metadataClass must not be null.");
        Validation.notNull(instanceClass, "instanceClass must not be null.");
        return mustDependOn(
                STATUS_ENABLED,
                true,
                true,
                false,
                null,
                it -> {
                    Deque<Class<?>> markedClasses = it.markedClasses.get(metadataClass);
                    if (markedClasses == null || markedClasses.isEmpty()) {
                        return null;
                    }
                    InstanceFactory<M, ?> instanceFactory =
                            instanceFactoryBy(metadataClass, instanceClass);
                    if (instanceFactory == null) {
                        return null;
                    }
                    for (Class<?> markedClass : markedClasses) {
                        M metadata = markedClass.getAnnotation(metadataClass);
                        Validation.notNull(metadata, "metadata must not be null.");
                        assert metadata != null;
                        ComponentKey componentKey =
                                new ComponentKey(metadataClass, markedClass, instanceFactory);
                        SimpleComponent<M, T> component =
                                (SimpleComponent<M, T>) components.get(componentKey);
                        if (component != null) {
                            return component;
                        }
                        if (!instanceFactory.canBeCreated(it, metadata, markedClass)) {
                            continue;
                        }
                        if (!it.inProgress.add(componentKey)) {
                            throw new IllegalStateException("component is in progress. (circular)");
                        }
                        if (!NextLazy.class.isAssignableFrom(metadataClass)
                                && markedClass.getAnnotation(NextLazy.class) == null) {
                            T instance = (T) instanceFactory.create(it, metadata, markedClass);
                            component = new SimpleComponent<>(metadata, instance);
                        } else {
                            component =
                                    new SimpleComponent<>(
                                            metadata,
                                            Lazy.of(
                                                    () ->
                                                            (T)
                                                                    instanceFactory.create(
                                                                            it,
                                                                            metadata,
                                                                            markedClass)));
                        }
                        components.put(componentKey, component);
                        it.inProgress.remove(componentKey);
                        return component;
                    }
                    return null;
                });
    }

    @NotNull @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <M extends Annotation> List<Component<M, ?>> componentsBy(
            @NotNull Class<M> metadataClass) {
        return (List) componentsBy(metadataClass, Object.class);
    }

    @NotNull @Override
    @SuppressWarnings({"unchecked", "DuplicatedCode"})
    public <M extends Annotation, T> List<Component<M, T>> componentsBy(
            @NotNull Class<M> metadataClass, @NotNull Class<T> instanceClass) {
        Validation.notNull(metadataClass, "metadataClass must not be null.");
        Validation.notNull(instanceClass, "instanceClass must not be null.");
        return mustDependOn(
                STATUS_ENABLED,
                true,
                true,
                false,
                Collections.emptyList(),
                it -> {
                    Deque<Class<?>> markedClasses = it.markedClasses.get(metadataClass);
                    if (markedClasses == null || markedClasses.isEmpty()) {
                        return Collections.emptyList();
                    }
                    InstanceFactory<M, ?> instanceFactory =
                            instanceFactoryBy(metadataClass, instanceClass);
                    if (instanceFactory == null) {
                        return Collections.emptyList();
                    }
                    List<Component<M, T>> components = new ArrayList<>(markedClasses.size());
                    for (Class<?> markedClass : markedClasses) {
                        M metadata = markedClass.getAnnotation(metadataClass);
                        Validation.notNull(metadata, "metadata must not be null.");
                        assert metadata != null;
                        ComponentKey componentKey =
                                new ComponentKey(metadataClass, markedClass, instanceFactory);
                        SimpleComponent<M, T> component =
                                (SimpleComponent<M, T>) it.components.get(componentKey);
                        if (component != null) {
                            components.add(component);
                            continue;
                        }
                        if (!instanceFactory.canBeCreated(it, metadata, markedClass)) {
                            continue;
                        }
                        if (!it.inProgress.add(componentKey)) {
                            throw new IllegalStateException("component is in progress. (circular)");
                        }
                        if (!NextLazy.class.isAssignableFrom(metadataClass)
                                && markedClass.getAnnotation(NextLazy.class) == null) {
                            T instance = (T) instanceFactory.create(it, metadata, markedClass);
                            component = new SimpleComponent<>(metadata, instance);
                        } else {
                            component =
                                    new SimpleComponent<>(
                                            metadata,
                                            Lazy.of(
                                                    () ->
                                                            (T)
                                                                    instanceFactory.create(
                                                                            it,
                                                                            metadata,
                                                                            markedClass)));
                        }
                        it.components.put(componentKey, component);
                        it.inProgress.remove(componentKey);
                        components.add(component);
                    }
                    return components;
                });
    }

    @Nullable @Override
    @SuppressWarnings({"unchecked"})
    public <T> Component<NextConfiguration, T> configurationBy(@NotNull Class<T> instanceClass) {
        Component<NextConfiguration, T> component =
                componentBy(NextConfiguration.class, instanceClass);
        if (component != null) {
            return component;
        }
        List<Component<NextConfiguration, Object>> components =
                componentsBy(NextConfiguration.class, Object.class);
        if (components.isEmpty()) {
            return null;
        }
        for (Component<NextConfiguration, Object> objectComponent : components) {
            if (instanceClass.isInstance(objectComponent.getInstance())) {
                return (Component<NextConfiguration, T>) objectComponent;
            }
        }
        return null;
    }

    @NotNull @Override
    @SuppressWarnings({"unchecked"})
    public <T> List<Component<NextConfiguration, T>> configurationsBy(
            @NotNull Class<T> instanceClass) {
        List<Component<NextConfiguration, T>> components =
                componentsBy(NextConfiguration.class, instanceClass);
        if (components.isEmpty()) {
            return Collections.emptyList();
        }
        List<Component<NextConfiguration, Object>> objectComponents =
                componentsBy(NextConfiguration.class, Object.class);
        if (objectComponents.isEmpty()) {
            return Collections.emptyList();
        }
        components = new ArrayList<>(objectComponents.size());
        for (Component<NextConfiguration, Object> objectComponent : objectComponents) {
            if (instanceClass.isInstance(objectComponent.getInstance())) {
                components.add((Component<NextConfiguration, T>) objectComponent);
            }
        }
        return components;
    }

    private void doInitialize() {
        setInstanceFactory(NextCommand.class, new NextCommandInstanceFactory());
        setInstanceFactory(NextPlaceholder.class, new NextPlaceholderInstanceFactory());
    }

    private void doLoad() throws Throwable {
        Class<? extends ContextHolder> owner = contextHolder.getClass();
        Banner.lines(owner).forEach(Log::info);
        Bundled.release(owner, getDataFolder());
        if (instanceFactories.isEmpty()) {
            return;
        }
        URL location = owner.getProtectionDomain().getCodeSource().getLocation();
        File file = Paths.get(location.toURI()).toFile();
        List<String> classPaths = new LinkedList<>();
        Functional.functional(new JarFile(file))
                .use(
                        jar -> {
                            Enumeration<JarEntry> entries = jar.entries();
                            String ownerName = owner.getName().replace('.', '/');
                            while (entries.hasMoreElements()) {
                                JarEntry entry = entries.nextElement();
                                if (entry.isDirectory()) {
                                    continue;
                                }
                                String entryName = entry.getName();
                                if (!entryName.endsWith(".class")) {
                                    continue;
                                }
                                String className =
                                        entryName
                                                .substring(0, entryName.length() - 6)
                                                .replace('\\', '/');
                                if (className.equals(ownerName)) {
                                    continue;
                                }
                                AtomicBoolean isClass = new AtomicBoolean(false);
                                Functional.functional(jar.getInputStream(entry))
                                        .use(
                                                input -> {
                                                    ClassReader classReader =
                                                            new ClassReader(input);
                                                    isClass.set(
                                                            className.equals(
                                                                    classReader.getClassName()));
                                                });
                                if (!isClass.get()) {
                                    continue;
                                }
                                classPaths.add(entryName);
                            }
                        });
        if (classPaths.isEmpty()) {
            return;
        }
        JavaCache javaCache = new JavaCache(file, classPaths.size() * 2, 0.75f);
        ClassLoader ownerClassLoader = owner.getClassLoader();
        for (String classPath : classPaths) {
            JavaClass javaClass = Java.typeof(classPath, javaCache, ownerClassLoader);
            JavaAnnotation[] annotations = javaClass.getAnnotations();
            for (JavaAnnotation annotation : annotations) {
                JavaClass annotationType = annotation.getAnnotationType();
                String annotationTypeName = annotationType.getName();
                for (Class<? extends Annotation> metadataClass : instanceFactories.keySet()) {
                    if (annotationTypeName.equals(metadataClass.getName())) {
                        Class<?> markedClass = javaClass.java(ownerClassLoader);
                        Deque<Class<?>> classes =
                                this.markedClasses.computeIfAbsent(
                                        metadataClass, k -> new ConcurrentLinkedDeque<>());
                        classes.add(markedClass);
                        Log.debug(() -> metadataClass.getName() + ": " + markedClass.getName());
                    }
                }
            }
        }
    }

    private void doEnable() {}

    private void doDisable() {}

    private void doDestroy() {
        if (components.isEmpty()) {
            return;
        }
        int count = 0;
        for (SimpleComponent<?, ?> component : components.values()) {
            try {
                if (!component.isInitialized()) {
                    continue;
                }
                Object instance = component.getInstance();
                if (instance instanceof Closeable) {
                    ((Closeable) instance).close();
                }
            } catch (Throwable e) {
                Log.error(e);
                count++;
            }
        }
        components.clear();
        if (count > 0) {
            throw new IllegalStateException("failed to destroy " + count + " components.");
        }
    }

    @RequiredArgsConstructor
    @Getter(AccessLevel.PRIVATE)
    private static final class ComponentKey {
        @NonNull private final Class<? extends Annotation> metadataClass;
        @NonNull private final Class<?> markedClass;
        @NonNull private final InstanceFactory<?, ?> instanceFactory;

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ComponentKey that = (ComponentKey) o;
            return Objects.equals(getMetadataClass(), that.getMetadataClass())
                    && Objects.equals(getMarkedClass(), that.getMarkedClass())
                    && Objects.equals(
                            System.identityHashCode(getInstanceFactory()),
                            System.identityHashCode(that.getInstanceFactory()));
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    getMetadataClass(),
                    getMarkedClass(),
                    System.identityHashCode(getInstanceFactory()));
        }
    }
}
