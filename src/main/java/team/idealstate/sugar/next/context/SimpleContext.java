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

import static team.idealstate.sugar.next.function.Functional.functional;
import static team.idealstate.sugar.next.function.Functional.optional;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import team.idealstate.sugar.banner.Banner;
import team.idealstate.sugar.bundled.Bundled;
import team.idealstate.sugar.internal.org.objectweb.asm.ClassReader;
import team.idealstate.sugar.logging.Log;
import team.idealstate.sugar.next.bytecode.Java;
import team.idealstate.sugar.next.bytecode.JavaCache;
import team.idealstate.sugar.next.bytecode.api.member.JavaClass;
import team.idealstate.sugar.next.bytecode.api.struct.JavaAnnotation;
import team.idealstate.sugar.next.bytecode.exception.BytecodeParsingException;
import team.idealstate.sugar.next.context.annotation.component.Component;
import team.idealstate.sugar.next.context.annotation.component.Configuration;
import team.idealstate.sugar.next.context.annotation.feature.Autowired;
import team.idealstate.sugar.next.context.annotation.feature.DependsOn;
import team.idealstate.sugar.next.context.annotation.feature.Environment;
import team.idealstate.sugar.next.context.annotation.feature.Named;
import team.idealstate.sugar.next.context.annotation.feature.Qualifier;
import team.idealstate.sugar.next.context.annotation.feature.RegisterFactories;
import team.idealstate.sugar.next.context.annotation.feature.RegisterFactory;
import team.idealstate.sugar.next.context.annotation.feature.RegisterProperties;
import team.idealstate.sugar.next.context.annotation.feature.RegisterProperty;
import team.idealstate.sugar.next.context.annotation.feature.Scan;
import team.idealstate.sugar.next.context.annotation.feature.Scope;
import team.idealstate.sugar.next.context.aware.Aware;
import team.idealstate.sugar.next.context.aware.BeanNameAware;
import team.idealstate.sugar.next.context.aware.ContextAware;
import team.idealstate.sugar.next.context.aware.ContextHolderAware;
import team.idealstate.sugar.next.context.aware.EventBusAware;
import team.idealstate.sugar.next.context.aware.MarkedAware;
import team.idealstate.sugar.next.context.aware.MetadataAware;
import team.idealstate.sugar.next.context.aware.SelfAware;
import team.idealstate.sugar.next.context.exception.ContextException;
import team.idealstate.sugar.next.context.factory.ComponentBeanFactory;
import team.idealstate.sugar.next.context.factory.ConfigurationBeanFactory;
import team.idealstate.sugar.next.context.lifecycle.Destroyable;
import team.idealstate.sugar.next.context.lifecycle.Initializable;
import team.idealstate.sugar.next.databind.Property;
import team.idealstate.sugar.next.eventbus.EventBus;
import team.idealstate.sugar.next.function.Lazy;
import team.idealstate.sugar.next.function.closure.Function;
import team.idealstate.sugar.next.function.closure.Provider;
import team.idealstate.sugar.next.io.IOUtils;
import team.idealstate.sugar.next.reflect.Reflection;
import team.idealstate.sugar.string.StringUtils;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;
import team.idealstate.sugar.validate.annotation.Nullable;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class SimpleContext implements Context {
    private static final int STATUS_ERROR = -1;
    private static final int STATUS_DESTROYED = 0;
    private static final int STATUS_INITIALIZED = 1;
    private static final int STATUS_LOADED = 2;
    private static final int STATUS_ENABLED = 3;
    private static final int STATUS_DISABLED = 4;

    @NonNull
    private final ContextHolder contextHolder;

    @NonNull
    private final ContextLifecycle contextLifecycle;

    @NonNull
    private final EventBus eventBus;

    private volatile int status = STATUS_DESTROYED;
    private final Lock lock = new ReentrantLock();
    private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
    private static final long TIMEOUT = 1000L;

    private <R> R mustDependOn(
            int depend, boolean allowTimeout, boolean next, R def, Function<SimpleContext, R> function) {
        int current = this.status;
        try {
            if (current > STATUS_ERROR) {
                Validation.is(current >= depend, "status must be " + depend + ".");
            }
        } catch (Throwable e) {
            throw new ContextException(e);
        }
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
        try {
            R result = function.call(this);
            if (next && current > STATUS_ERROR) {
                if (current + 1 > STATUS_DISABLED) {
                    this.status = STATUS_DESTROYED;
                    this.environment = null;
                } else {
                    this.status = current + 1;
                }
            }
            return result;
        } catch (Throwable e) {
            this.status = STATUS_ERROR;
            throw new ContextException(e);
        } finally {
            lock.unlock();
        }
    }

    private volatile String environment = null;

    @NotNull
    @Override
    public String getEnvironment() {
        String environment = this.environment;
        if (environment != null) {
            return environment;
        }
        return (this.environment = System.getProperty(PROPERTY_ENVIRONMENT_KEY, ""));
    }

    @Override
    @NotNull
    public ContextHolder getHolder() {
        return contextHolder;
    }

    @NotNull
    @Override
    public ClassLoader getClassLoader() {
        return contextHolder.getClass().getClassLoader();
    }

    private final Map<String, ContextProperty> properties = new LinkedHashMap<>();

    @Nullable
    @Override
    public ContextProperty getProperty(@NotNull String key) {
        Validation.notNullOrBlank(key, "key must not be null or blank.");
        return properties.get(key);
    }

    @Override
    public void registerProperty(@NotNull String key, @NotNull String value) {
        Validation.notNullOrBlank(key, "key must not be null or blank.");
        Validation.notNull(value, "value must not be null.");
        mustDependOn(STATUS_INITIALIZED, false, false, null, (it) -> {
            Log.debug(() -> String.format("Register property: '%s' = '%s'", key, value));
            it.properties.put(key, new SimpleContextProperty(Property.of(key, value)));
            return null;
        });
    }

    @NotNull
    @Override
    public String getName() {
        return Validation.requireNotNullOrBlank(contextHolder.getName(), "name must not be null or blank.");
    }

    @NotNull
    @Override
    public String getVersion() {
        return Validation.requireNotNullOrBlank(contextHolder.getVersion(), "version must not be null or blank.");
    }

    @NotNull
    @Override
    public File getDataFolder() {
        return Validation.requireNotNull(contextHolder.getDataFolder(), "data folder must not be null.");
    }

    private static JarFile getJarFile(@NotNull Class<?> holder) throws IOException {
        CodeSource codeSource = holder.getProtectionDomain().getCodeSource();
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
        } catch (URISyntaxException e) {
            throw new ContextException(e);
        }
        return new JarFile(file);
    }

    @Nullable
    @Override
    public InputStream getResource(@NotNull String uri, @NotNull Class<?> holder, @NotNull ClassLoader classLoader)
            throws IOException {
        Validation.notNullOrBlank(uri, "uri must not be null or blank.");
        Validation.notNull(holder, "holder must not be null.");
        Validation.notNull(classLoader, "classLoader must not be null.");
        uri = uri.replace('\\', '/');
        if (uri.startsWith(RESOURCE_CLASSPATH)) {
            uri = uri.substring(RESOURCE_CLASSPATH.length());
            if (uri.charAt(0) == '/') {
                uri = uri.substring(1);
            }
            return classLoader.getResourceAsStream(uri);
        }
        if (uri.startsWith(RESOURCE_CONTEXT)) {
            uri = uri.substring(RESOURCE_CONTEXT.length());
            holder = getHolder().getClass();
            try (JarFile jarFile = getJarFile(holder)) {
                if (jarFile == null) {
                    return null;
                }
                if (uri.startsWith("/")) {
                    uri = uri.substring(1);
                }
                String finalUri = uri;
                Class<?> finalHolder = holder;
                return optional(functional(jarFile).use(InputStream.class, jar -> {
                            JarEntry entry = jar.getJarEntry(finalUri);
                            if (entry == null) {
                                return null;
                            }
                            return new ByteArrayInputStream(IOUtils.readAllBytes(jar.getInputStream(entry)));
                        }))
                        .orElseGet(() -> finalHolder.getClassLoader().getResourceAsStream(finalUri));
            }
        }
        if (uri.startsWith(RESOURCE_BUNDLED)) {
            uri = uri.substring(RESOURCE_BUNDLED.length());
            try (JarFile jarFile = getJarFile(holder)) {
                if (jarFile == null) {
                    return null;
                }
                if (uri.startsWith("/")) {
                    uri = uri.substring(1);
                }
                String finalUri = uri;
                return functional(jarFile).use(InputStream.class, jar -> {
                    JarEntry entry = jar.getJarEntry(finalUri);
                    if (entry == null) {
                        return null;
                    }
                    return new ByteArrayInputStream(IOUtils.readAllBytes(jar.getInputStream(entry)));
                });
            }
        }
        URI u;
        try {
            u = new URI(uri);
        } catch (URISyntaxException e) {
            throw new ContextException(e);
        }
        if (u.isAbsolute()) {
            return u.toURL().openStream();
        }
        File file = new File(getDataFolder(), uri);
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
        mustDependOn(STATUS_DESTROYED, false, true, null, it -> {
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
        mustDependOn(STATUS_INITIALIZED, false, true, null, it -> {
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
        mustDependOn(STATUS_LOADED, false, true, null, it -> {
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
        mustDependOn(STATUS_ENABLED, false, true, null, it -> {
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
        mustDependOn(STATUS_DISABLED, false, true, null, it -> {
            it.doBeforeDestroy();
            it.contextLifecycle.onDestroy(this);
            it.doDestroy();
            it.contextLifecycle.onDestroyed(this);
            it.doAfterDestroy();
            return null;
        });
    }

    private final Map<Class<? extends Annotation>, BeanFactory<?>> beanFactories = new LinkedHashMap<>();

    @Nullable
    @Override
    public <M extends Annotation> BeanFactory<?> getBeanFactory(@NotNull Class<M> metadataType) {
        Validation.notNull(metadataType, "metadataType must not be null.");
        BeanFactory<?> beanFactory = beanFactories.get(metadataType);
        if (beanFactory == null) {
            if (!Component.class.equals(metadataType) && isComponentAnnotation(metadataType)) {
                return beanFactories.get(Component.class);
            }
            return null;
        }
        return beanFactory;
    }

    @Override
    public <M extends Annotation> void registerBeanFactory(
            @NotNull Class<M> metadataType, @NotNull BeanFactory<M> beanFactory) {
        Validation.notNull(metadataType, "metadataType must not be null.");
        Validation.notNull(beanFactory, "beanFactory must not be null.");
        Class<M> beanFactoryMetadataType = beanFactory.getMetadataType();
        Validation.notNull(beanFactoryMetadataType, "beanFactory.getMetadataType() must not be null.");
        Validation.is(
                metadataType.equals(beanFactoryMetadataType),
                String.format(
                        "metadataType '%s' must be equal to beanFactory.getMetadataType() '%s'.",
                        metadataType, beanFactoryMetadataType));
        mustDependOn(STATUS_INITIALIZED, false, false, null, (it) -> {
            Log.debug(() ->
                    String.format("Registering bean factory '%s' for metadata type '%s'.", beanFactory, metadataType));
            it.beanFactories.put(metadataType, beanFactory);
            return null;
        });
    }

    private final Map<String, SimpleBean<?>> nameMap = new LinkedHashMap<>();
    private final Map<Class<?>, SimpleBean<?>> markedMap = new LinkedHashMap<>();
    private final Deque<Object> instances = new ConcurrentLinkedDeque<>();
    private final LinkedHashSet<Class<?>> inProgress = new LinkedHashSet<>();

    @Nullable
    @Override
    @SuppressWarnings({"unchecked"})
    public <T> Bean<T> getBean(@NotNull String beanName, @NotNull Class<T> beanType) {
        Validation.notNullOrBlank(beanName, "beanName must not be blank.");
        Validation.notNull(beanType, "beanType must not be null.");
        return mustDependOn(STATUS_LOADED, true, false, null, it -> {
            SimpleBean<?> bean = it.nameMap.get(beanName);
            if (bean == null) {
                return null;
            }
            if (beanType.isAssignableFrom(bean.getMarked())) {
                return (Bean<T>) bean;
            }
            return null;
        });
    }

    @Nullable
    @Override
    @SuppressWarnings({"unchecked"})
    public <T> Bean<T> getBean(@NotNull Class<T> beanType) {
        Validation.notNull(beanType, "beanType must not be null.");
        return mustDependOn(STATUS_LOADED, true, false, null, it -> {
            SimpleBean<?> bean = it.markedMap.get(beanType);
            if (bean == null) {
                for (Map.Entry<Class<?>, SimpleBean<?>> entry : it.markedMap.entrySet()) {
                    Class<?> marked = entry.getKey();
                    if (beanType.isAssignableFrom(marked)) {
                        return (Bean<T>) entry.getValue();
                    }
                }
            }
            return (Bean<T>) bean;
        });
    }

    @NotNull
    @SuppressWarnings({"unchecked"})
    public <T> List<Bean<T>> getBeans(@NotNull Class<T> beanType) {
        Validation.notNull(beanType, "beanType must not be null.");
        return mustDependOn(STATUS_LOADED, true, false, Collections.emptyList(), it -> {
            List<Bean<T>> result = new LinkedList<>();
            for (Map.Entry<Class<?>, SimpleBean<?>> entry : it.markedMap.entrySet()) {
                Class<?> marked = entry.getKey();
                if (beanType.isAssignableFrom(marked)) {
                    result.add((Bean<T>) entry.getValue());
                }
            }
            return result;
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <M extends Annotation, T> T doCreate(
            @NotNull BeanFactory<M> beanFactory,
            @NotNull String beanName,
            @Nullable DependsOn dependsOn,
            @NotNull M metadata,
            @NotNull Class<T> marked) {
        T result = null;
        try {
            long[] start = {System.currentTimeMillis(), System.currentTimeMillis()};
            Log.debug(() -> String.format(
                    "creating bean. (beanName='%s', marked='%s', beanFactory='%s')", beanName, marked, beanFactory));
            if (!inProgress.add(marked)) {
                throw new IllegalStateException(String.format(
                        "circular dependency detected. (beanName='%s', marked='%s') %s", beanName, marked, inProgress));
            }
            Log.debug(() -> String.format("create instance. (beanName='%s', metadata='%s')", beanName, metadata));
            T instance = beanFactory.create(this, beanName, metadata, marked);
            result = instance;
            Validation.notNull(instance, "Instance must not be null.");
            Class<?> instanceType = instance.getClass();
            Validation.is(
                    marked.isInstance(instance),
                    String.format("Instance '%s' must be an instance of '%s'.", instanceType, marked));
            Log.debug(() -> String.format(
                    "(%s ms) created instance. (beanName='%s', instanceType='%s')",
                    System.currentTimeMillis() - start[1], beanName, instanceType));
            if (instance instanceof Aware) {
                start[1] = System.currentTimeMillis();
                Log.debug(() -> String.format("inject aware. (beanName='%s')", beanName));
                if (instance instanceof ContextAware) {
                    ((ContextAware) instance).setContext(this);
                }
                if (instance instanceof ContextHolderAware) {
                    ((ContextHolderAware) instance).setContextHolder(contextHolder);
                }
                if (instance instanceof EventBusAware) {
                    ((EventBusAware) instance).setEventBus(eventBus);
                }
                if (instance instanceof MetadataAware) {
                    ((MetadataAware) instance).setMetadata(metadata);
                }
                if (instance instanceof BeanNameAware) {
                    ((BeanNameAware) instance).setBeanName(beanName);
                }
                if (instance instanceof MarkedAware) {
                    ((MarkedAware) instance).setMarkedClass(marked);
                }
                Log.debug(() -> String.format(
                        "(%s ms) injected aware. (beanName='%s')", System.currentTimeMillis() - start[1], beanName));
            }
            Method[] methods = instanceType.getMethods();
            T proxy;
            if (methods.length != 0) {
                start[1] = System.currentTimeMillis();
                Log.debug(() -> String.format("autowire methods. (beanName='%s')", beanName));
                doAutowire(instance, instanceType, methods);
                Log.debug(() -> String.format(
                        "(%s ms) autowired methods. (beanName='%s')", System.currentTimeMillis() - start[1], beanName));
                start[1] = System.currentTimeMillis();
                Log.debug(() -> String.format("maybe proxy. (beanName='%s')", beanName));
                proxy = maybeProxy(beanFactory, beanName, metadata, instance, marked);
                Validation.notNull(proxy, "Proxy must not be null.");
                Validation.is(
                        marked.isInstance(proxy),
                        String.format("Proxy '%s' must be an instance of '%s'.", proxy.getClass(), marked));
                Log.debug(() -> String.format(
                        "(%s ms) maybe proxied. (beanName='%s')", System.currentTimeMillis() - start[1], beanName));
            } else {
                proxy = instance;
            }
            result = proxy;
            if (instance instanceof SelfAware) {
                start[1] = System.currentTimeMillis();
                Log.debug(() -> String.format("inject self. (beanName='%s')", beanName));
                ((SelfAware) instance).setSelf(proxy);
                Log.debug(() -> String.format(
                        "(%s ms) injected self. (beanName='%s')", System.currentTimeMillis() - start[1], beanName));
            }
            if (proxy instanceof Initializable) {
                start[1] = System.currentTimeMillis();
                Log.debug(() -> String.format("initialize bean. (beanName='%s')", beanName));
                ((Initializable) proxy).initialize();
                Log.debug(() -> String.format(
                        "(%s ms) initialized bean. (beanName='%s')", System.currentTimeMillis() - start[1], beanName));
            }
            Log.debug(() -> String.format(
                    "(%s ms) created bean. (beanName='%s', runtimeType='%s')",
                    System.currentTimeMillis() - start[0], beanName, proxy.getClass()));
            inProgress.remove(marked);
        } finally {
            if (result != null) {
                instances.add(result);
            }
        }
        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void doAutowire(Object instance, Class<?> instanceType, Method[] methods) {
        for (Method method : methods) {
            if (method.getAnnotation(Autowired.class) == null) {
                continue;
            }
            String methodName = method.getName();
            String instanceTypeName = instanceType.getName();
            if (Modifier.isStatic(method.getModifiers())) {
                Log.warn(String.format("Autowire: '%s' static method '%s' is ignored.", instanceTypeName, methodName));
                continue;
            }
            long start = System.currentTimeMillis();
            Parameter[] parameters = method.getParameters();
            Object[] parameterValues = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                String parameterName = parameter.getName();
                Class<?> parameterType = parameter.getType();
                Class<?> autowireType = getAutowireType(instanceTypeName, methodName, parameterName, parameterType);
                Object value = null;
                Qualifier qualifier = parameter.getAnnotation(Qualifier.class);
                if (Bean.class.equals(parameterType)) {
                    List<Bean<?>> autowireValue =
                            getAutowireValue(qualifier, parameterName, (Class) autowireType, true);
                    if (!autowireValue.isEmpty()) {
                        value = autowireValue.get(0);
                    }
                } else if (Lazy.class.equals(parameterType)) {
                    List<Bean<?>> autowireValue =
                            getAutowireValue(qualifier, parameterName, (Class) autowireType, true);
                    if (!autowireValue.isEmpty()) {
                        Bean<?> bean = autowireValue.get(0);
                        value = Lazy.of(bean::getInstance);
                    }
                } else if (List.class.equals(parameterType)) {
                    List<Bean<?>> autowireValue =
                            getAutowireValue(qualifier, parameterName, (Class) autowireType, false);
                    if (autowireValue.isEmpty()) {
                        value = Collections.emptyList();
                    } else {
                        value = autowireValue.stream().map(Bean::getInstance).collect(Collectors.toList());
                    }
                } else if (Map.class.equals(parameterType)) {
                    List<Bean<?>> autowireValue =
                            getAutowireValue(qualifier, parameterName, (Class) autowireType, false);
                    if (autowireValue.isEmpty()) {
                        value = Collections.emptyMap();
                    } else {
                        value = autowireValue.stream().collect(Collectors.toMap(Bean::getName, Bean::getInstance));
                    }
                } else {
                    List<Bean<?>> autowireValue =
                            getAutowireValue(qualifier, parameterName, (Class) autowireType, true);
                    if (!autowireValue.isEmpty()) {
                        value = autowireValue.get(0).getInstance();
                    }
                }
                if (parameter.isAnnotationPresent(NotNull.class)) {
                    Validation.notNull(
                            value,
                            String.format(
                                    "Autowire: '%s' method '%s' parameter '%s' value must not be null.",
                                    instanceTypeName, methodName, parameterName));
                }
                parameterValues[i] = value;
            }
            method.setAccessible(true);
            try {
                Log.debug(() -> String.format("Autowire: '%s' method '%s'...", instanceTypeName, methodName));
                method.invoke(instance, parameterValues);
                Log.debug(() -> String.format(
                        "(%s) Autowire: '%s' method '%s' done.",
                        System.currentTimeMillis() - start, instanceTypeName, methodName));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new ContextException(e);
            }
        }
    }

    @NotNull
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> List<Bean<T>> getAutowireValue(
            Qualifier qualifier, String parameterName, Class<T> autowireType, boolean one) {
        if (one || qualifier != null) {
            Bean bean;
            if (qualifier == null) {
                bean = getBean(autowireType);
            } else {
                String beanName = qualifier.value();
                bean = getBean(StringUtils.isNullOrBlank(beanName) ? parameterName : beanName, autowireType);
            }
            return bean == null ? Collections.emptyList() : Collections.singletonList(bean);
        }
        return getBeans(autowireType);
    }

    @NotNull
    private Class<?> getAutowireType(
            String instanceTypeName, String methodName, String parameterName, Class<?> parameterType) {
        Class<?> type = null;
        if (Bean.class.equals(parameterType) || List.class.equals(parameterType) || Lazy.class.equals(parameterType)) {
            TypeVariable<? extends Class<?>>[] typeParameters = parameterType.getTypeParameters();
            Validation.is(
                    typeParameters.length == 1,
                    String.format(
                            "Autowire: '%s' method '%s' parameter '%s' must specify one type" + " variable.",
                            instanceTypeName, methodName, parameterName));
            Type[] bounds = typeParameters[0].getBounds();
            for (Type bound : bounds) {
                if (isArrayOrGenericOrNonClass(bound)) {
                    continue;
                }
                type = (Class<?>) bound;
                break;
            }
            Validation.not(
                    isArrayOrGenericOrNonClass(type),
                    String.format(
                            "Autowire: '%s' method '%s' parameter '%s' element type must not be an"
                                    + " array or a generic.",
                            instanceTypeName, methodName, parameterName));
        } else if (Map.class.equals(parameterType)) {
            TypeVariable<? extends Class<?>>[] typeParameters = parameterType.getTypeParameters();
            Validation.is(
                    typeParameters.length == 2,
                    String.format(
                            "Autowire: '%s' method '%s' parameter '%s' must specify two type" + " variables.",
                            instanceTypeName, methodName, parameterName));
            TypeVariable<? extends Class<?>> typeVariable = typeParameters[0];
            for (Type bound : typeVariable.getBounds()) {
                if (isArrayOrGenericOrNonClass(bound)) {
                    continue;
                }
                type = (Class<?>) bound;
                break;
            }
            Validation.not(
                    isArrayOrGenericOrNonClass(type),
                    String.format(
                            "Autowire: '%s' method '%s' parameter '%s' key type must not be an"
                                    + " array or a generic.",
                            instanceTypeName, methodName, parameterName));
            assert type != null;
            Validation.is(
                    type.isAssignableFrom(String.class),
                    String.format(
                            "Autowire: '%s' method '%s' parameter '%s' key type must be assignable" + " from String.",
                            instanceTypeName, methodName, parameterName));
            typeVariable = typeParameters[1];
            type = null;
            for (Type bound : typeVariable.getBounds()) {
                if (isArrayOrGenericOrNonClass(bound)) {
                    continue;
                }
                type = (Class<?>) bound;
                break;
            }
            Validation.not(
                    isArrayOrGenericOrNonClass(type),
                    String.format(
                            "Autowire: '%s' method '%s' parameter '%s' value type must not be an"
                                    + " array or a generic.",
                            instanceTypeName, methodName, parameterName));
            assert type != null;
            return type;
        }
        Validation.not(
                isArrayOrGenericOrNonClass(parameterType),
                String.format(
                        "Autowire: '%s' method '%s' parameter '%s' must not be an array or a" + " generic.",
                        instanceTypeName, methodName, parameterName));
        return parameterType;
    }

    private boolean isArrayOrGenericOrNonClass(Type type) {
        if (!(type instanceof Class)) {
            return true;
        }
        Class<?> cls = (Class<?>) type;
        return cls.isArray() || isGeneric(cls);
    }

    private boolean isGeneric(Class<?> cls) {
        return cls.getTypeParameters().length != 0;
    }

    @NotNull
    private <M extends Annotation, T> T maybeProxy(
            @NotNull BeanFactory<M> beanFactory,
            @NotNull String beanName,
            @NotNull M metadata,
            @NotNull T instance,
            @NotNull Class<T> marked) {
        T proxy = beanFactory.proxy(this, beanName, metadata, instance, marked);
        Class<?> instanceType = instance.getClass();
        Validation.notNull(proxy, "Proxy must not be null.");
        Validation.is(
                instanceType.isInstance(proxy),
                String.format("proxy '%s' must be an instance of '%s'.", proxy.getClass(), instanceType));
        return proxy;
    }

    private void doBeforeInitialize() {
        getEnvironment();
    }

    private void doInitialize() {}

    private void doAfterInitialize() {}

    private void doBeforeLoad() {
        registerBeanFactory(Component.class, new ComponentBeanFactory());
        registerBeanFactory(Configuration.class, new ConfigurationBeanFactory());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void doLoad() throws Throwable {
        ContextHolder holder = getHolder();
        Class<? extends ContextHolder> owner = holder.getClass();
        Banner.lines(owner).forEach(Log::info);
        Log.info(String.format("Environment: '%s'", getEnvironment()));
        Bundled.release(owner, getDataFolder());
        Set<String> scanPackages = loadBoots(holder);
        Set<File> bootFiles = loadBootFiles(holder);
        long[] start = {System.currentTimeMillis()};
        Log.debug(() -> String.format(
                "Scanning packages...\n%s",
                functional(new StringJoiner("\n", "[\n", "\n]"))
                        .apply(it -> scanPackages.forEach(it::add))
                        .it()));
        ClassLoader ownerClassLoader = owner.getClassLoader();
        Map<String, DependsOn> dependOnMap = new LinkedHashMap<>(bootFiles.size() * 64);
        Set<String> inDependOnDone = new LinkedHashSet<>(bootFiles.size() * 64);
        Set<String> duplicate = new HashSet<>(bootFiles.size() * 64);
        Log.info("Register beans ...");
        for (File bootFile : bootFiles) {
            Set<String> classes = loadBootClasses(holder, bootFile, scanPackages);
            if (classes.isEmpty()) {
                continue;
            }
            JavaCache javaCache = new JavaCache(bootFile, classes.size());
            COLLECTION:
            for (String className : classes) {
                Log.debug(() -> String.format("Class name: %s", className));
                JavaClass javaClass = Java.typeof(className, javaCache, ownerClassLoader);
                JavaAnnotation[] javaAnnotations = javaClass.getAnnotations();
                Set<JavaAnnotation> maybeMetadataAnnotations = Collections.emptySet();
                COLLECTION_METADATA:
                for (JavaAnnotation javaAnnotation : javaAnnotations) {
                    JavaClass annotationType;
                    try {
                        annotationType = javaAnnotation.getAnnotationType();
                    } catch (BytecodeParsingException e) {
                        if (e.getCause() instanceof ClassNotFoundException) {
                            continue;
                        }
                        throw e;
                    }
                    for (Class<? extends Annotation> metadataType : beanFactories.keySet()) {
                        if (metadataType.getName().equals(annotationType.getName())) {
                            if (maybeMetadataAnnotations.isEmpty()) {
                                maybeMetadataAnnotations = new LinkedHashSet<>(javaAnnotations.length);
                            }
                            maybeMetadataAnnotations.add(javaAnnotation);
                            continue COLLECTION_METADATA;
                        }
                    }
                    if (maybeComponentAnnotation(javaAnnotation)) {
                        if (maybeMetadataAnnotations.isEmpty()) {
                            maybeMetadataAnnotations = new LinkedHashSet<>(javaAnnotations.length);
                        }
                        maybeMetadataAnnotations.add(javaAnnotation);
                    }
                }
                if (maybeMetadataAnnotations.isEmpty()) {
                    continue;
                }
                Validation.is(
                        maybeMetadataAnnotations.size() == 1,
                        String.format(
                                "Class '%s' has multiple annotations of metadata type. %s",
                                className, maybeMetadataAnnotations));
                JavaAnnotation maybeComponentAnnotation =
                        maybeMetadataAnnotations.stream().findFirst().get();
                Class<? extends Annotation> metadataType =
                        maybeComponentAnnotation.getAnnotationType().java(ownerClassLoader);
                BeanFactory beanFactory = getBeanFactory(metadataType);
                if (beanFactory == null) {
                    Log.debug(String.format("No bean factory for metadata '%s', skip", metadataType));
                    continue;
                }
                Class<?> marked = javaClass.java(ownerClassLoader);
                Annotation metadata;
                if (metadataType.equals(beanFactory.getMetadataType())) {
                    metadata = marked.getDeclaredAnnotation(metadataType);
                } else if (Component.class.equals(beanFactory.getMetadataType())) {
                    metadata = Reflection.annotation(Component.class, maybeComponentAnnotation.getMappings());
                } else {
                    throw new UnsupportedOperationException(String.format(
                            "Unsupported metadata type '%s' for bean factory '%s'.", metadataType, beanFactory));
                }
                if (metadata == null) {
                    throw new IllegalStateException(
                            String.format("Class '%s' metadata '%s' is invisible.", className, metadataType));
                }
                if (!duplicate.add(className)) {
                    throw new IllegalStateException(String.format("Duplicate class name: '%s'", className));
                }
                Environment environment = marked.getAnnotation(Environment.class);
                if (environment != null) {
                    String env = environment.value();
                    if (!StringUtils.isEmpty(env) && !getEnvironment().equals(env)) {
                        continue;
                    }
                }
                Named named = marked.getAnnotation(Named.class);
                String beanName;
                if (named == null || StringUtils.isNullOrBlank(named.value())) {
                    beanName = marked.getName();
                } else {
                    beanName = named.value();
                }
                if (nameMap.containsKey(beanName)) {
                    throw new IllegalStateException(String.format("Bean name '%s' is duplicated.", beanName));
                }
                DependsOn dependsOn = marked.getAnnotation(DependsOn.class);
                ClassLoader markedClassLoader = marked.getClassLoader();
                if (dependsOn != null) {
                    for (String dependClassName : dependsOn.classes()) {
                        try {
                            Class.forName(dependClassName, false, markedClassLoader);
                        } catch (ClassNotFoundException e) {
                            Log.debug(() -> String.format("No found depend class '%s', skip.", dependClassName));
                            continue COLLECTION;
                        }
                    }
                    for (DependsOn.Property property : dependsOn.properties()) {
                        ContextProperty contextProperty = getProperty(property.key());
                        if (contextProperty == null
                                || (property.strict() && !property.value().equals(contextProperty.getValue()))) {
                            Log.debug(() -> String.format(
                                    "Depend property '%s' is not set or not equal to" + " '%s', skip.",
                                    property.key(), property.value()));
                            continue COLLECTION;
                        }
                    }
                }
                if (!beanFactory.validate(this, beanName, metadata, marked)) {
                    Log.warn(String.format("Bean factory '%s' rejects bean '%s'.", beanFactory.getClass(), marked));
                    continue;
                }
                Provider<Object> provider;
                Scope scope = marked.getAnnotation(Scope.class);
                if (scope == null) {
                    scope = Reflection.annotation(Scope.class, Collections.singletonMap("value", Scope.DEFAULT));
                }
                if (Scope.PROTOTYPE.equals(scope.value())) {
                    provider = () -> doCreate(beanFactory, beanName, dependsOn, metadata, marked);
                } else {
                    provider = Lazy.of(() -> doCreate(beanFactory, beanName, dependsOn, metadata, marked));
                }
                SimpleBean<?> bean =
                        new SimpleBean<>(beanName, scope, dependsOn, metadataType, metadata, (Class) marked, provider);
                nameMap.put(beanName, bean);
                markedMap.put(marked, bean);
                if (dependsOn != null) {
                    dependOnMap.put(beanName, dependsOn);
                } else {
                    inDependOnDone.add(beanName);
                }
                Log.info(String.format("%s: '%s'.", metadataType.getSimpleName(), beanName));
            }
        }
        Log.info("Register beans done.");
        if (!dependOnMap.isEmpty()) {
            for (String beanName : dependOnMap.keySet()) {
                resolveDependsOnMap(beanName, dependOnMap, inDependOnDone, new LinkedHashSet<>(dependOnMap.size()));
            }
            dependOnMap.keySet().removeIf(inDependOnDone::contains);
            if (!dependOnMap.isEmpty()) {
                for (Map.Entry<String, DependsOn> entry : dependOnMap.entrySet()) {
                    String beanName = entry.getKey();
                    DependsOn dependsOn = entry.getValue();
                    Log.debug(() -> String.format("Bean '%s' dependsOn: '%s'", beanName, dependsOn));
                    markedMap.remove(nameMap.remove(beanName).getMarked());
                    Log.warn(() -> String.format("Bean '%s' dependsOn is not resolved, skip.", beanName));
                }
            }
        }
        Log.debug(() -> String.format("(%s ms) Scanning package done.", System.currentTimeMillis() - start[0]));
    }

    private void resolveDependsOnMap(
            String beanName,
            Map<String, DependsOn> dependOnMap,
            Set<String> inDependOnDone,
            Set<String> inDependOnProgress) {
        if (inDependOnDone.contains(beanName)) {
            return;
        }
        DependsOn dependsOn = dependOnMap.get(beanName);
        if (dependsOn == null) {
            return;
        }
        if (!inDependOnProgress.add(beanName)) {
            throw new IllegalStateException(
                    String.format("(Circular) Bean '%s' is in dependsOn progress. %s", beanName, inDependOnProgress));
        }
        for (String dependBeanName : dependsOn.beans()) {
            if (inDependOnDone.contains(dependBeanName)) {
                continue;
            }
            resolveDependsOnMap(dependBeanName, dependOnMap, inDependOnDone, inDependOnProgress);
        }
        inDependOnDone.add(beanName);
        inDependOnProgress.remove(beanName);
    }

    private void doAfterLoad() {}

    private void doBeforeEnable() {}

    private void doEnable() {
        if (markedMap.isEmpty()) {
            return;
        }
        for (SimpleBean<?> bean : markedMap.values()) {
            String scope = bean.getScope().value();
            if (StringUtils.isBlank(scope)) {
                scope = Scope.DEFAULT;
            }
            if (Scope.SINGLETON.equals(scope)) {
                bean.getInstance();
            }
        }
    }

    private void doAfterEnable() {}

    private void doBeforeDisable() {}

    private void doDisable() {}

    private void doAfterDisable() {}

    private void doBeforeDestroy() {}

    private void doDestroy() {
        try {
            int count = 0;
            List<Object> instances = new ArrayList<>(this.instances);
            Collections.reverse(instances);
            for (Object instance : instances) {
                try {
                    if (instance instanceof Destroyable) {
                        ((Destroyable) instance).destroy();
                    }
                } catch (Throwable e) {
                    Log.error(e);
                    count++;
                }
            }
            if (count > 0) {
                throw new IllegalStateException("Failed to destroy " + count + " instances.");
            }
        } finally {
            properties.clear();
            beanFactories.clear();
            nameMap.clear();
            markedMap.clear();
            instances.clear();
            inProgress.clear();
        }
    }

    private void doAfterDestroy() {}

    private static boolean maybeComponentAnnotation(@NotNull JavaAnnotation javaAnnotation) {
        Validation.notNull(javaAnnotation, "javaAnnotation must not be null.");
        JavaClass annotationType;
        try {
            annotationType = javaAnnotation.getAnnotationType();
        } catch (BytecodeParsingException e) {
            if (e.getCause() instanceof ClassNotFoundException) {
                return false;
            }
            throw e;
        }
        String componentClassName = Component.class.getName();
        if (componentClassName.equals(annotationType.getName())) {
            return true;
        }
        JavaAnnotation[] javaAnnotations = annotationType.getAnnotations();
        for (JavaAnnotation annotation : javaAnnotations) {
            try {
                annotationType = annotation.getAnnotationType();
            } catch (BytecodeParsingException e) {
                if (e.getCause() instanceof ClassNotFoundException) {
                    continue;
                }
                throw e;
            }
            if (componentClassName.equals(annotationType.getName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isComponentAnnotation(@NotNull Class<?> annotationType) {
        Validation.notNull(annotationType, "Annotation type must not be null.");
        if (!annotationType.isAnnotation()) {
            return false;
        }
        if (Component.class.equals(annotationType)) {
            return true;
        }
        return annotationType.isAnnotationPresent(Component.class);
    }

    @NotNull
    private List<Annotation> loadBootMetadata() {
        Annotation[] annotations = contextHolder.getClass().getAnnotations();
        if (annotations.length == 0) {
            return Collections.emptyList();
        }
        List<Annotation> result = new LinkedList<>();
        for (Annotation annotation : annotations) {
            result.add(annotation);
            Class<? extends Annotation> annotationType = annotation.annotationType();
            Annotation[] annotationTypeAnnotations = annotationType.getAnnotations();
            if (annotationTypeAnnotations.length == 0) {
                continue;
            }
            result.addAll(Arrays.asList(annotationTypeAnnotations));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private Set<String> loadBoots(@NotNull ContextHolder holder)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Validation.notNull(holder, "Context holder must not be null.");
        Class<? extends ContextHolder> owner = holder.getClass();
        Set<String> scanPackages = new HashSet<>();
        for (Annotation annotation : loadBootMetadata()) {
            if (annotation instanceof Scan) {
                Scan scan = (Scan) annotation;
                String[] value = scan.value();
                if (value.length != 0) {
                    scanPackages.addAll(Arrays.stream(value)
                            .map(s -> s.replace('\\', '/').replace('.', '/'))
                            .collect(Collectors.toList()));
                }
            } else if (annotation instanceof RegisterFactory) {
                RegisterFactory registerFactory = (RegisterFactory) annotation;
                registerBeanFactory(
                        registerFactory.metadata(),
                        registerFactory.beanFactory().getConstructor().newInstance());
            } else if (annotation instanceof RegisterProperty) {
                RegisterProperty registerProperty = (RegisterProperty) annotation;
                registerProperty(registerProperty.key(), registerProperty.value());
            } else if (annotation instanceof RegisterFactories) {
                RegisterFactories registerFactories = (RegisterFactories) annotation;
                for (RegisterFactory registerFactory : registerFactories.value()) {
                    registerBeanFactory(
                            registerFactory.metadata(),
                            registerFactory.beanFactory().getConstructor().newInstance());
                }
            } else if (annotation instanceof RegisterProperties) {
                RegisterProperties registerProperties = (RegisterProperties) annotation;
                for (RegisterProperty registerProperty : registerProperties.value()) {
                    registerProperty(registerProperty.key(), registerProperty.value());
                }
            }
        }
        scanPackages.add(owner.getPackage().getName().replace('.', '/'));
        return scanPackages;
    }

    @NotNull
    private Set<File> loadBootFiles(@NotNull ContextHolder holder) throws URISyntaxException {
        Validation.notNull(holder, "Context holder must not be null.");
        Class<? extends ContextHolder> owner = holder.getClass();
        CodeSource codeSource = owner.getProtectionDomain().getCodeSource();
        Validation.notNull(codeSource, String.format("Context holder '%s' must have a code source.", owner));
        URL location = codeSource.getLocation();
        Validation.notNull(location, String.format("Context holder '%s' must have a location.", owner));
        File ownerFile = new File(location.toURI());
        Annotation[] annotations = owner.getAnnotations();
        Set<File> result = new LinkedHashSet<>(annotations.length + 1);
        for (Annotation annotation : annotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType.isAnnotationPresent(Scan.class)) {
                codeSource = annotationType.getProtectionDomain().getCodeSource();
                if (codeSource == null) {
                    continue;
                }
                location = codeSource.getLocation();
                if (location == null) {
                    continue;
                }
                File bootFile = new File(location.toURI());
                if (!bootFile.exists()
                        || bootFile.isDirectory()
                        || !bootFile.getName().endsWith(".jar")) {
                    continue;
                }
                result.add(bootFile);
            }
        }
        result.add(ownerFile);
        return result;
    }

    @NotNull
    private Set<String> loadBootClasses(
            @NotNull ContextHolder holder, @NotNull File bootFile, @NotNull Set<String> scanPackages)
            throws IOException {
        Validation.notNull(holder, "Context holder must not be null.");
        Validation.notNull(bootFile, "Boot file must not be null.");
        Validation.notNull(scanPackages, "Scan packages must not be null.");
        if (scanPackages.isEmpty()) {
            return Collections.emptySet();
        }
        String owner = holder.getClass().getName().replace('.', '/');
        Set<String> result = new LinkedHashSet<>(64);
        functional(new JarFile(bootFile)).use(jar -> {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String entryName = entry.getName();
                if (!entryName.endsWith(".class") || scanPackages.stream().noneMatch(entryName::startsWith)) {
                    continue;
                }
                String className =
                        entryName.substring(0, entryName.length() - 6).replace('\\', '/');
                if (className.equals(owner)) {
                    continue;
                }
                if (!functional(jar.getInputStream(entry))
                        .use(Boolean.class, input -> className.equals(new ClassReader(input).getClassName()))) {
                    continue;
                }
                result.add(entryName);
            }
        });
        return result;
    }
}
