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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;
import org.objectweb.asm.ClassReader;
import team.idealstate.minecraft.next.common.banner.Banner;
import team.idealstate.minecraft.next.common.bundled.Bundled;
import team.idealstate.minecraft.next.common.bytecode.Java;
import team.idealstate.minecraft.next.common.bytecode.JavaCache;
import team.idealstate.minecraft.next.common.bytecode.api.member.JavaClass;
import team.idealstate.minecraft.next.common.bytecode.api.struct.JavaAnnotation;
import team.idealstate.minecraft.next.common.bytecode.exception.BytecodeParsingException;
import team.idealstate.minecraft.next.common.context.annotation.component.Component;
import team.idealstate.minecraft.next.common.context.annotation.component.Configuration;
import team.idealstate.minecraft.next.common.context.annotation.component.Controller;
import team.idealstate.minecraft.next.common.context.annotation.component.Subscriber;
import team.idealstate.minecraft.next.common.context.annotation.feature.Autowire;
import team.idealstate.minecraft.next.common.context.annotation.feature.Autowired;
import team.idealstate.minecraft.next.common.context.annotation.feature.Environment;
import team.idealstate.minecraft.next.common.context.annotation.feature.Laziness;
import team.idealstate.minecraft.next.common.context.annotation.feature.Transaction;
import team.idealstate.minecraft.next.common.context.aware.Aware;
import team.idealstate.minecraft.next.common.context.aware.ContextAware;
import team.idealstate.minecraft.next.common.context.aware.ContextHolderAware;
import team.idealstate.minecraft.next.common.context.aware.EventBusAware;
import team.idealstate.minecraft.next.common.context.aware.MarkedAware;
import team.idealstate.minecraft.next.common.context.aware.MetadataAware;
import team.idealstate.minecraft.next.common.context.aware.SelfAware;
import team.idealstate.minecraft.next.common.context.exception.ContextException;
import team.idealstate.minecraft.next.common.context.factory.ComponentBeanFactory;
import team.idealstate.minecraft.next.common.context.factory.ConfigurationBeanFactory;
import team.idealstate.minecraft.next.common.context.factory.ControllerBeanFactory;
import team.idealstate.minecraft.next.common.context.factory.SubscriberBeanFactory;
import team.idealstate.minecraft.next.common.context.lifecycle.Destroyable;
import team.idealstate.minecraft.next.common.context.lifecycle.Initializable;
import team.idealstate.minecraft.next.common.database.DatabaseSessionFactory;
import team.idealstate.minecraft.next.common.eventbus.EventBus;
import team.idealstate.minecraft.next.common.function.Lazy;
import team.idealstate.minecraft.next.common.function.closure.Function;
import team.idealstate.minecraft.next.common.io.InputUtils;
import team.idealstate.minecraft.next.common.logging.Log;
import team.idealstate.minecraft.next.common.string.StringUtils;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;
import team.idealstate.minecraft.next.common.validate.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
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

import static team.idealstate.minecraft.next.common.function.Functional.functional;

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
    private final DatabaseSessionFactory databaseSessionFactory;
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
            if (safe) {
                lock.unlock();
            }
        }
    }

    private volatile String environment = null;

    @NotNull @Override
    public String getEnvironment() {
        String environment = this.environment;
        if (environment != null) {
            return environment;
        }
        return (this.environment = System.getProperty(PROPERTY_ENVIRONMENT_KEY, ""));
    }

    @NotNull
    @Override
    public ClassLoader getClassLoader() {
        return contextHolder.getClass().getClassLoader();
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

    @Nullable
    @Override
    public DatabaseSessionFactory getDatabaseSessionFactory() {
        return databaseSessionFactory;
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

    private final Map<Class<? extends Annotation>, BeanFactory<?, ?>> instanceFactories =
            new ConcurrentHashMap<>();

    @Nullable @Override
    public <M extends Annotation> BeanFactory<M, ?> getBeanFactory(@NotNull Class<M> metadataType) {
        return getBeanFactory(metadataType, Object.class);
    }

    @Nullable @Override
    @SuppressWarnings({"unchecked"})
    public <M extends Annotation, T> BeanFactory<M, T> getBeanFactory(
            @NotNull Class<M> metadataType, @NotNull Class<T> beanType) {
        Validation.notNull(metadataType, "metadataType must not be null.");
        Validation.notNull(beanType, "beanType must not be null.");
        BeanFactory<?, ?> beanFactory = instanceFactories.get(metadataType);
        if (beanFactory != null) {
            Validation.is(
                    metadataType.isAssignableFrom(beanFactory.getMetadataType()),
                    "metadataType must be assignable to beanFactory.getMetadataType()");
            if (!beanType.isAssignableFrom(beanFactory.getInstanceType())) {
                return null;
            }
        }
        return (BeanFactory<M, T>) beanFactory;
    }

    @Override
    public <M extends Annotation> void setBeanFactory(
            @NotNull Class<M> metadataType, @Nullable BeanFactory<M, ?> beanFactory) {
        Validation.notNull(metadataType, "metadataType must not be null");
        Validation.is(
                !Laziness.class.isAssignableFrom(metadataType),
                "metadataType must not be Laziness");
        Validation.is(
                !Environment.class.isAssignableFrom(metadataType),
                "metadataType must not be Environment");
        Validation.notNull(beanFactory, "beanFactory must not be null");
        if (beanFactory != null) {
            Class<M> factoryMetadataClass = beanFactory.getMetadataType();
            Validation.is(
                    factoryMetadataClass.isAssignableFrom(metadataType),
                    "metadataType must be assignable to beanFactory.getMetadataType()");
            instanceFactories.put(metadataType, beanFactory);
        } else {
            instanceFactories.remove(metadataType);
        }
    }

    private final Map<Class<? extends Annotation>, Deque<Class<?>>> markedClasses =
            new ConcurrentHashMap<>();
    private final Map<ComponentKey, SimpleBean<?, ?>> components = new ConcurrentHashMap<>();
    private final Set<ComponentKey> inProgress = new CopyOnWriteArraySet<>();

    @Nullable @Override
    public <M extends Annotation> Bean<M, ?> getBean(@NotNull Class<M> metadataType) {
        return getBean(metadataType, Object.class);
    }

    @Nullable @Override
    @SuppressWarnings({"unchecked", "DuplicatedCode"})
    public <M extends Annotation, T> Bean<M, T> getBean(
            @NotNull Class<M> metadataType, @NotNull Class<T> beanType) {
        Validation.notNull(metadataType, "metadataType must not be null.");
        Validation.notNull(beanType, "beanType must not be null.");
        return (Bean<M, T>)
                mustDependOn(
                        STATUS_ENABLED,
                        true,
                        true,
                        false,
                        null,
                        it -> {
                            Deque<Class<?>> markedClasses = it.markedClasses.get(metadataType);
                            if (markedClasses == null || markedClasses.isEmpty()) {
                                return null;
                            }
                            BeanFactory<M, ?> beanFactory = getBeanFactory(metadataType, beanType);
                            Class<?> activedBeanType = beanType;
                            if (beanFactory == null) {
                                if (Object.class.equals(beanType)) {
                                    return null;
                                }
                                beanFactory = getBeanFactory(metadataType, Object.class);
                                if (beanFactory == null) {
                                    return null;
                                }
                                activedBeanType = Object.class;
                            }
                            for (Class<?> marked : markedClasses) {
                                M metadata = marked.getAnnotation(metadataType);
                                Validation.notNull(metadata, "metadata must not be null.");
                                assert metadata != null;
                                ComponentKey componentKey =
                                        new ComponentKey(metadataType, marked, beanFactory);
                                SimpleBean<M, ?> component =
                                        (SimpleBean<M, ?>) components.get(componentKey);
                                if (component != null) {
                                    if (activedBeanType.equals(beanType)
                                            || beanType.isInstance(component.getInstance())) {
                                        it.components.put(componentKey, component);
                                        return component;
                                    }
                                    continue;
                                }
                                if (!beanFactory.canBeCreated(it, metadata, marked)) {
                                    continue;
                                }
                                Environment componentEnv = marked.getAnnotation(Environment.class);
                                if (componentEnv != null) {
                                    String env = componentEnv.value();
                                    if (!StringUtils.isEmpty(env)
                                            && !it.getEnvironment().equalsIgnoreCase(env)) {
                                        continue;
                                    }
                                }
                                if (!it.inProgress.add(componentKey)) {
                                    throw new IllegalStateException(
                                            "component is in progress. (circular)");
                                }
                                if (marked.getAnnotation(Laziness.class) == null) {
                                    T instance = (T) doCreate(it, beanFactory, metadata, marked);
                                    component = new SimpleBean<>(metadata, instance);
                                } else {
                                    BeanFactory<M, ?> finalBeanFactory = beanFactory;
                                    component =
                                            new SimpleBean<>(
                                                    metadata,
                                                    Lazy.of(
                                                            () ->
                                                                    doCreate(
                                                                            it,
                                                                            finalBeanFactory,
                                                                            metadata,
                                                                            marked)));
                                }
                                if (activedBeanType.equals(beanType)
                                        || beanType.isInstance(component.getInstance())) {
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
    public <M extends Annotation> List<Bean<M, ?>> getBeans(@NotNull Class<M> metadataType) {
        return (List) getBeans(metadataType, Object.class);
    }

    @NotNull @Override
    @SuppressWarnings({"unchecked", "rawtypes", "DuplicatedCode"})
    public <M extends Annotation, T> List<Bean<M, T>> getBeans(
            @NotNull Class<M> metadataType, @NotNull Class<T> beanType) {
        Validation.notNull(metadataType, "metadataType must not be null.");
        Validation.notNull(beanType, "beanType must not be null.");
        return mustDependOn(
                STATUS_ENABLED,
                true,
                true,
                false,
                Collections.emptyList(),
                it -> {
                    Deque<Class<?>> markedClasses = it.markedClasses.get(metadataType);
                    if (markedClasses == null || markedClasses.isEmpty()) {
                        return Collections.emptyList();
                    }
                    BeanFactory<M, ?> beanFactory = getBeanFactory(metadataType, beanType);
                    Class<?> activedBeanType = beanType;
                    if (beanFactory == null) {
                        if (Object.class.equals(beanType)) {
                            return Collections.emptyList();
                        }
                        beanFactory = getBeanFactory(metadataType, Object.class);
                        if (beanFactory == null) {
                            return Collections.emptyList();
                        }
                        activedBeanType = Object.class;
                    }
                    List<Bean<M, T>> beans = new ArrayList<>(markedClasses.size());
                    for (Class<?> marked : markedClasses) {
                        M metadata = marked.getAnnotation(metadataType);
                        Validation.notNull(metadata, "metadata must not be null.");
                        assert metadata != null;
                        ComponentKey componentKey =
                                new ComponentKey(metadataType, marked, beanFactory);
                        SimpleBean<M, ?> component =
                                (SimpleBean<M, ?>) it.components.get(componentKey);
                        if (component != null) {
                            if (activedBeanType.equals(beanType)
                                    || beanType.isInstance(component.getInstance())) {
                                beans.add((SimpleBean) component);
                            }
                            continue;
                        }
                        if (!beanFactory.canBeCreated(it, metadata, marked)) {
                            continue;
                        }
                        Environment componentEnv = marked.getAnnotation(Environment.class);
                        if (componentEnv != null) {
                            String env = componentEnv.value();
                            if (!StringUtils.isEmpty(env)
                                    && !it.getEnvironment().equalsIgnoreCase(env)) {
                                continue;
                            }
                        }
                        if (!it.inProgress.add(componentKey)) {
                            throw new IllegalStateException("component is in progress. (circular)");
                        }
                        if (marked.getAnnotation(Laziness.class) == null) {
                            T instance = (T) doCreate(it, beanFactory, metadata, marked);
                            component = new SimpleBean<>(metadata, instance);
                        } else {
                            BeanFactory<M, ?> finalBeanFactory = beanFactory;
                            component =
                                    new SimpleBean<>(
                                            metadata,
                                            Lazy.of(
                                                    () ->
                                                            doCreate(
                                                                    it,
                                                                    finalBeanFactory,
                                                                    metadata,
                                                                    marked)));
                        }
                        if (activedBeanType.equals(beanType)
                                || beanType.isInstance(component.getInstance())) {
                            it.components.put(componentKey, component);
                            beans.add((SimpleBean) component);
                        }
                        it.inProgress.remove(componentKey);
                    }
                    return beans;
                });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <M extends Annotation, T> T doCreate(
            @NotNull Context context,
            @NotNull BeanFactory<M, T> beanFactory,
            @NotNull M metadata,
            @NotNull Class<?> marked) {
        T instance = beanFactory.create(context, metadata, marked);
        Validation.notNull(instance, "instance must not be null.");
        if (instance instanceof Aware) {
            if (instance instanceof ContextAware) {
                ((ContextAware) instance).setContext(context);
            }
            if (instance instanceof MetadataAware) {
                ((MetadataAware) instance).setMetadata(metadata);
            }
            if (instance instanceof MarkedAware) {
                ((MarkedAware) instance).setMarkedClass(marked);
            }
            if (instance instanceof ContextHolderAware) {
                ((ContextHolderAware) instance).setContextHolder(contextHolder);
            }
            if (instance instanceof EventBusAware) {
                ((EventBusAware) instance).setEventBus(eventBus);
            }
        }
        Class<?> instanceType = instance.getClass();
        Method[] methods = instanceType.getMethods();
        T proxy;
        if (methods.length != 0) {
            doAutowire(instance, instanceType, methods);
            proxy = maybeProxy(instance, instanceType, methods);
        } else {
            proxy = instance;
        }
        if (instance instanceof SelfAware) {
            ((SelfAware) instance).setSelf(proxy);
        }
        if (proxy instanceof Initializable) {
            ((Initializable) proxy).initialize();
        }
        return proxy;
    }

    private void doAutowire(Object instance, Class<?> instanceType, Method[] methods) {
        for (Method method : methods) {
            if (method.getAnnotation(Autowired.class) == null) {
                continue;
            }
            String methodName = method.getName();
            String instanceTypeName = instanceType.getName();
            if (Modifier.isStatic(method.getModifiers())) {
                Log.warn(
                        String.format(
                                "Autowire: '%s' static method '%s' is ignored.",
                                instanceTypeName, methodName));
                continue;
            }
            Parameter[] parameters = method.getParameters();
            Object[] parameterValues = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                Autowire autowire = parameter.getAnnotation(Autowire.class);
                String parameterName = parameter.getName();
                if (autowire == null) {
                    throw new IllegalStateException(
                            String.format(
                                    "Autowire: '%s' method '%s' parameter '%s' must be annotated"
                                            + " with @Autowire.",
                                    instanceTypeName, methodName, parameterName));
                }
                Class<?> autowireInstanceType = autowire.instanceType();
                Class<?> parameterType = parameter.getType();
                String autowireInstanceTypeName = autowireInstanceType.getName();
                Class<? extends Annotation> metadataType = autowire.metadataType();
                Object value;
                boolean required = autowire.required();
                if (autowire.multiple()) {
                    Validation.is(
                            List.class.isAssignableFrom(parameterType),
                            String.format(
                                    "Autowire: '%s' method '%s' parameter '%s' must be assignable"
                                            + " to List<? super %s>.",
                                    instanceTypeName,
                                    methodName,
                                    parameterName,
                                    autowireInstanceTypeName));
                    value = getBeans(metadataType, autowireInstanceType);
                    if (required && ((List<?>) value).isEmpty()) {
                        throw new IllegalStateException(
                                String.format(
                                        "Autowire: '%s' method '%s' parameter '%s' must not be"
                                                + " empty.",
                                        instanceTypeName, methodName, parameterName));
                    }
                } else {
                    Validation.is(
                            parameterType.isAssignableFrom(autowireInstanceType),
                            String.format(
                                    "Autowire: '%s' method '%s' parameter '%s' must be assignable"
                                            + " from %s.",
                                    instanceTypeName,
                                    methodName,
                                    parameterName,
                                    autowireInstanceTypeName));
                    value = getBean(metadataType, autowireInstanceType);
                    if (required && value == null) {
                        throw new IllegalStateException(
                                String.format(
                                        "Autowire: '%s' method '%s' parameter '%s' must not be"
                                                + " null.",
                                        instanceTypeName, methodName, parameterName));
                    }
                }
                parameterValues[i] = value;
            }
            method.setAccessible(true);
            try {
                method.invoke(instance, parameterValues);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new ContextException(e);
            }
        }
    }

    @NotNull
    @SuppressWarnings({"unchecked"})
    private <T> T maybeProxy(T instance, Class<?> instanceType, Method[] methods) {
        Map<String, Transaction> transactions = new HashMap<>(methods.length);
        for (Method method : methods) {
            Transaction transaction = method.getAnnotation(Transaction.class);
            if (transaction == null) {
                continue;
            }
            if (Modifier.isStatic(method.getModifiers())) {
                Log.warn(String.format(
                        "Transaction: '%s' static method '%s' is ignored.",
                        instanceType.getName(), method.getName()
                ));
                continue;
            }
            transactions.put(method.toString(), transaction);
        }
        if (transactions.isEmpty()) {
            return instance;
        }
        transactions = Collections.unmodifiableMap(transactions);
        Class<?> proxyType = functional(new ByteBuddy().subclass(instanceType)
                .method(ElementMatchers.any().and(ElementMatchers.not(ElementMatchers.isStatic())))
                .intercept(MethodDelegation.to(new TransactionInterceptor(this, instance, transactions)))
                .make())
                .use(Class.class, unloaded -> unloaded.load(getClassLoader()).getLoaded());
        try {
            return (T) proxyType.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new ContextException(e);
        }
    }

    private void doBeforeInitialize() {
        getEnvironment();
    }

    private void doInitialize() {}

    private void doAfterInitialize() {
        setBeanFactory(Component.class, new ComponentBeanFactory());
        setBeanFactory(Configuration.class, new ConfigurationBeanFactory());
        setBeanFactory(Controller.class, new ControllerBeanFactory());
        setBeanFactory(Subscriber.class, new SubscriberBeanFactory());
    }

    private void doBeforeLoad() {}

    private void doLoad() throws Throwable {
        Class<? extends ContextHolder> owner = contextHolder.getClass();
        Banner.lines(owner).forEach(Log::info);
        Log.info(String.format("Environment: '%s'", getEnvironment()));
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
        for (SimpleBean<?, ?> component : components.values()) {
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
        @NonNull private final Class<? extends Annotation> metadataType;
        @NonNull private final Class<?> marked;
        @NonNull private final BeanFactory<?, ?> beanFactory;

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ComponentKey that = (ComponentKey) o;
            return Objects.equals(getMetadataType(), that.getMetadataType())
                    && Objects.equals(getMarked(), that.getMarked())
                    && Objects.equals(
                            System.identityHashCode(getBeanFactory()),
                            System.identityHashCode(that.getBeanFactory()));
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    getMetadataType(), getMarked(), System.identityHashCode(getBeanFactory()));
        }
    }

    @RequiredArgsConstructor
    private static final class TransactionInterceptor {
        @NonNull
        private final Context context;
        @NonNull
        private final Object instance;
        @NonNull
        private final Map<String, Transaction> transactions;

        @RuntimeType
        public Object intercept(@Origin String method, @Origin MethodHandle methodHandle, @AllArguments Object[] arguments) throws Throwable {
            Transaction transaction = transactions.get(method);
            MethodHandle bound = methodHandle.bindTo(instance);
            if (transaction == null) {
                return bound.invokeWithArguments(arguments);
            }
            DatabaseSessionFactory databaseSessionFactory = context.getDatabaseSessionFactory();
            if (databaseSessionFactory == null) {
                throw new IllegalStateException("database session factory is not set.");
            }
            return functional(databaseSessionFactory.openSession(transaction.executionMode(), transaction.isolationLevel()))
                    .use(Object.class, session -> {
                        try {
                            return bound.invokeWithArguments(arguments);
                        } catch (Throwable e) {
                            session.rollback();
                            throw e;
                        }
                    });
        }
    }
}
