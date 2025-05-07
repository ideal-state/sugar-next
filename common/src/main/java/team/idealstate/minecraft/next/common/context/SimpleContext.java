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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
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
import java.util.concurrent.atomic.AtomicReference;
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
import team.idealstate.minecraft.next.common.bytecode.exception.BytecodeParsingException;
import team.idealstate.minecraft.next.common.context.annotation.NextLazy;
import team.idealstate.minecraft.next.common.context.annotation.component.NextCommand;
import team.idealstate.minecraft.next.common.context.annotation.component.NextComponent;
import team.idealstate.minecraft.next.common.context.annotation.component.NextConfiguration;
import team.idealstate.minecraft.next.common.context.annotation.component.NextEventSubscriber;
import team.idealstate.minecraft.next.common.context.annotation.component.NextPlaceholder;
import team.idealstate.minecraft.next.common.context.aware.Aware;
import team.idealstate.minecraft.next.common.context.aware.ContextAware;
import team.idealstate.minecraft.next.common.context.aware.ContextHolderAware;
import team.idealstate.minecraft.next.common.context.aware.EventBusAware;
import team.idealstate.minecraft.next.common.context.aware.MetadataAware;
import team.idealstate.minecraft.next.common.context.exception.ContextException;
import team.idealstate.minecraft.next.common.context.factory.NextCommandInstanceFactory;
import team.idealstate.minecraft.next.common.context.factory.NextComponentInstanceFactory;
import team.idealstate.minecraft.next.common.context.factory.NextConfigurationYamlInstanceFactory;
import team.idealstate.minecraft.next.common.context.factory.NextEventSubscriberInstanceFactory;
import team.idealstate.minecraft.next.common.context.factory.NextPlaceholderInstanceFactory;
import team.idealstate.minecraft.next.common.context.lifecycle.Destroyable;
import team.idealstate.minecraft.next.common.context.lifecycle.Initializable;
import team.idealstate.minecraft.next.common.eventbus.EventBus;
import team.idealstate.minecraft.next.common.function.Lazy;
import team.idealstate.minecraft.next.common.function.closure.Function;
import team.idealstate.minecraft.next.common.io.InputUtils;
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
    @NonNull private final ContextLifecycle contextLifecycle;
    @NonNull private final EventBus eventBus;
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
            Validation.is(current > STATUS_ERROR, "error status " + current + ".");
            Validation.is(current == depend, "status must be " + depend + ".");
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

    @Nullable @Override
    public InputStream getResource(@NotNull String path) throws IOException {
        Validation.notNullOrBlank(path, "path must not be null or blank.");
        AtomicReference<InputStream> inputStream = new AtomicReference<>(null);
        Class<? extends @NonNull ContextHolder> owner = contextHolder.getClass();
        if (path.startsWith(RESOURCE_BUNDLED)) {
            path = path.substring(RESOURCE_BUNDLED.length());
            URL location = owner.getProtectionDomain().getCodeSource().getLocation();
            File file;
            try {
                file = Paths.get(location.toURI()).toFile();
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
            String finalPath = path;
            functional(new JarFile(file))
                    .use(
                            jar -> {
                                JarEntry entry = jar.getJarEntry(finalPath);
                                if (entry == null) {
                                    return;
                                }
                                inputStream.set(
                                        new ByteArrayInputStream(
                                                InputUtils.readStream(
                                                        jar.getInputStream(entry), true)));
                            });
            if (inputStream.get() != null) {
                return inputStream.get();
            }
            inputStream.set(owner.getResourceAsStream(path));
        }
        if (inputStream.get() != null) {
            return inputStream.get();
        }
        URI uri = URI.create(path);
        if (uri.isAbsolute()) {
            return uri.toURL().openStream();
        }
        File file = new File(getDataFolder(), path);
        if (file.exists()) {
            return Files.newInputStream(file.toPath());
        }
        return null;
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
                    it.doBeforeInitialize();
                    it.contextLifecycle.onInitialize(this);
                    it.doInitialize();
                    it.contextLifecycle.onInitialized(this);
                    it.doAfterInitialize();
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
                    it.doBeforeLoad();
                    it.contextLifecycle.onLoad(this);
                    it.doLoad();
                    it.contextLifecycle.onLoaded(this);
                    it.doAfterLoad();
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
                    it.doBeforeEnable();
                    it.contextLifecycle.onEnable(this);
                    it.doEnable();
                    it.contextLifecycle.onEnabled(this);
                    it.doAfterEnable();
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
                    it.doBeforeDisable();
                    it.contextLifecycle.onDisable(this);
                    it.doDisable();
                    it.contextLifecycle.onDisabled(this);
                    it.doAfterDisable();
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
                    it.doBeforeDestroy();
                    it.contextLifecycle.onDestroy(this);
                    it.doDestroy();
                    it.contextLifecycle.onDestroyed(this);
                    it.doAfterDestroy();
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
            Validation.is(
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
            @NotNull Class<M> metadataClass, InstanceFactory<M, ?> instanceFactory) {
        Validation.notNull(metadataClass, "metadataClass must not be null");
        Validation.is(
                !NextLazy.class.isAssignableFrom(metadataClass),
                "metadataClass must not be NextLazy");
        Validation.notNull(instanceFactory, "instanceFactory must not be null");
        if (instanceFactory != null) {
            Class<M> factoryMetadataClass = instanceFactory.getMetadataClass();
            Validation.is(
                    factoryMetadataClass.isAssignableFrom(metadataClass),
                    "metadataClass must be assignable to factoryMetadataClass");
            instanceFactories.put(metadataClass, instanceFactory);
        } else {
            instanceFactories.remove(metadataClass);
        }
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
        return (Component<M, T>)
                mustDependOn(
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
                            Class<?> activedClass = instanceClass;
                            if (instanceFactory == null) {
                                if (Object.class.equals(instanceClass)) {
                                    return null;
                                }
                                instanceFactory = instanceFactoryBy(metadataClass, Object.class);
                                if (instanceFactory == null) {
                                    return null;
                                }
                                activedClass = Object.class;
                            }
                            for (Class<?> markedClass : markedClasses) {
                                M metadata = markedClass.getAnnotation(metadataClass);
                                Validation.notNull(metadata, "metadata must not be null.");
                                assert metadata != null;
                                ComponentKey componentKey =
                                        new ComponentKey(
                                                metadataClass, markedClass, instanceFactory);
                                SimpleComponent<M, ?> component =
                                        (SimpleComponent<M, ?>) components.get(componentKey);
                                if (component != null) {
                                    if (activedClass.equals(instanceClass)
                                            || instanceClass.isInstance(component.getInstance())) {
                                        it.components.put(componentKey, component);
                                        return component;
                                    }
                                    continue;
                                }
                                if (!instanceFactory.canBeCreated(it, metadata, markedClass)) {
                                    continue;
                                }
                                if (!it.inProgress.add(componentKey)) {
                                    throw new IllegalStateException(
                                            "component is in progress. (circular)");
                                }
                                if (!NextLazy.class.isAssignableFrom(metadataClass)
                                        && markedClass.getAnnotation(NextLazy.class) == null) {
                                    T instance =
                                            (T)
                                                    doCreate(
                                                            it,
                                                            instanceFactory,
                                                            metadata,
                                                            markedClass);
                                    component = new SimpleComponent<>(metadata, instance);
                                } else {
                                    InstanceFactory<M, ?> finalInstanceFactory = instanceFactory;
                                    component =
                                            new SimpleComponent<>(
                                                    metadata,
                                                    Lazy.of(
                                                            () ->
                                                                    doCreate(
                                                                            it,
                                                                            finalInstanceFactory,
                                                                            metadata,
                                                                            markedClass)));
                                }
                                if (activedClass.equals(instanceClass)
                                        || instanceClass.isInstance(component.getInstance())) {
                                    it.components.put(componentKey, component);
                                    it.inProgress.remove(componentKey);
                                    return component;
                                }
                                it.inProgress.remove(componentKey);
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
    @SuppressWarnings({"unchecked", "rawtypes", "DuplicatedCode"})
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
                    Class<?> activedClass = instanceClass;
                    if (instanceFactory == null) {
                        if (Object.class.equals(instanceClass)) {
                            return Collections.emptyList();
                        }
                        instanceFactory = instanceFactoryBy(metadataClass, Object.class);
                        if (instanceFactory == null) {
                            return Collections.emptyList();
                        }
                        activedClass = Object.class;
                    }
                    List<Component<M, T>> components = new ArrayList<>(markedClasses.size());
                    for (Class<?> markedClass : markedClasses) {
                        M metadata = markedClass.getAnnotation(metadataClass);
                        Validation.notNull(metadata, "metadata must not be null.");
                        assert metadata != null;
                        ComponentKey componentKey =
                                new ComponentKey(metadataClass, markedClass, instanceFactory);
                        SimpleComponent<M, ?> component =
                                (SimpleComponent<M, ?>) it.components.get(componentKey);
                        if (component != null) {
                            if (activedClass.equals(instanceClass)
                                    || instanceClass.isInstance(component.getInstance())) {
                                components.add((SimpleComponent) component);
                            }
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
                            T instance = (T) doCreate(it, instanceFactory, metadata, markedClass);
                            component = new SimpleComponent<>(metadata, instance);
                        } else {
                            InstanceFactory<M, ?> finalInstanceFactory = instanceFactory;
                            component =
                                    new SimpleComponent<>(
                                            metadata,
                                            Lazy.of(
                                                    () ->
                                                            doCreate(
                                                                    it,
                                                                    finalInstanceFactory,
                                                                    metadata,
                                                                    markedClass)));
                        }
                        if (activedClass.equals(instanceClass)
                                || instanceClass.isInstance(component.getInstance())) {
                            it.components.put(componentKey, component);
                            components.add((SimpleComponent) component);
                        }
                        it.inProgress.remove(componentKey);
                    }
                    return components;
                });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <M extends Annotation, T> T doCreate(
            @NotNull Context context,
            @NotNull InstanceFactory<M, T> instanceFactory,
            @NotNull M metadata,
            @NotNull Class<?> marked) {
        T instance = instanceFactory.create(context, metadata, marked);
        Validation.notNull(instance, "instance must not be null.");
        if (instance instanceof Aware) {
            if (instance instanceof ContextAware) {
                ((ContextAware) instance).setContext(context);
            }
            if (instance instanceof MetadataAware) {
                ((MetadataAware) instance).setMetadata(metadata);
            }
            if (instance instanceof ContextHolderAware) {
                ((ContextHolderAware) instance).setContextHolder(contextHolder);
            }
            if (instance instanceof EventBusAware) {
                ((EventBusAware) instance).setEventBus(eventBus);
            }
        }
        if (instance instanceof Initializable) {
            ((Initializable) instance).initialize();
        }
        return instance;
    }

    private void doBeforeInitialize() {}

    private void doInitialize() {}

    private void doAfterInitialize() {
        setInstanceFactory(NextComponent.class, new NextComponentInstanceFactory());
        setInstanceFactory(NextConfiguration.class, new NextConfigurationYamlInstanceFactory());
        setInstanceFactory(NextCommand.class, new NextCommandInstanceFactory());
        setInstanceFactory(NextPlaceholder.class, new NextPlaceholderInstanceFactory());
        setInstanceFactory(NextEventSubscriber.class, new NextEventSubscriberInstanceFactory());
    }

    private void doBeforeLoad() {}

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
        functional(new JarFile(file))
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
                                functional(jar.getInputStream(entry))
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
                JavaClass annotationType;
                try {
                    annotationType = annotation.getAnnotationType();
                } catch (BytecodeParsingException e) {
                    if (e.getCause() instanceof ClassNotFoundException) {
                        continue;
                    }
                    throw e;
                }
                String annotationTypeName = annotationType.getName();
                for (Class<? extends Annotation> metadataClass : instanceFactories.keySet()) {
                    if (annotationTypeName.equals(metadataClass.getName())) {
                        Class<?> markedClass = javaClass.java(ownerClassLoader);
                        Deque<Class<?>> classes =
                                this.markedClasses.computeIfAbsent(
                                        metadataClass, k -> new ConcurrentLinkedDeque<>());
                        classes.add(markedClass);
                        Log.debug(
                                () -> metadataClass.getSimpleName() + ": " + markedClass.getName());
                    }
                }
            }
        }
    }

    private void doAfterLoad() {}

    private void doBeforeEnable() {}

    private void doEnable() {}

    private void doAfterEnable() {}

    private void doBeforeDisable() {}

    private void doDisable() {}

    private void doAfterDisable() {}

    private void doBeforeDestroy() {}

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
                if (instance instanceof Destroyable) {
                    ((Destroyable) instance).destroy();
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

    private void doAfterDestroy() {}

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
