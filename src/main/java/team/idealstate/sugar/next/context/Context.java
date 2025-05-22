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
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.List;
import team.idealstate.sugar.next.context.exception.ContextException;
import team.idealstate.sugar.next.eventbus.EventBus;
import team.idealstate.sugar.validate.annotation.NotNull;
import team.idealstate.sugar.validate.annotation.Nullable;

/** @see ContextException */
public interface Context {

    String RESOURCE_BUNDLED = "bundled:";
    String RESOURCE_CLASSPATH = "classpath:";
    String RESOURCE_CONTEXT = "context:";
    String PROPERTY_ENVIRONMENT_KEY = "sugar.next.environment";

    @NotNull
    static Context of(
            @NotNull ContextHolder contextHolder,
            @NotNull ContextLifecycle contextLifecycle,
            @NotNull EventBus eventBus) {
        return new SimpleContext(contextHolder, contextLifecycle, eventBus);
    }

    /**
     * @return 上下文所处的环境
     * @see #PROPERTY_ENVIRONMENT_KEY
     */
    @NotNull
    String getEnvironment();

    @NotNull
    ContextHolder getHolder();

    @NotNull
    ClassLoader getClassLoader();

    @Nullable
    ContextProperty getProperty(@NotNull String key);

    default boolean hasProperty(@NotNull String key) {
        return getProperty(key) != null;
    }

    void registerProperty(@NotNull String key, @NotNull String value);

    @NotNull
    String getName();

    @NotNull
    String getVersion();

    @NotNull
    File getDataFolder();

    /** @see #getResource(String, Class, ClassLoader) */
    default @Nullable InputStream getResource(@NotNull String uri) throws IOException {
        return getResource(uri, getHolder().getClass(), getClassLoader());
    }

    /**
     * @param uri 资源的 {@link URI}
     * @param classLoader 资源的 {@link ClassLoader}
     * @see #RESOURCE_CLASSPATH
     * @see #RESOURCE_BUNDLED
     * @see #RESOURCE_CONTEXT
     */
    @Nullable
    InputStream getResource(@NotNull String uri, @NotNull Class<?> holder, @NotNull ClassLoader classLoader)
            throws IOException;

    boolean isActive();

    void initialize();

    void load();

    void enable();

    void disable();

    void destroy();

    @Nullable
    <M extends Annotation> BeanFactory<?> getBeanFactory(@NotNull Class<M> metadataType);

    <M extends Annotation> void registerBeanFactory(
            @NotNull Class<M> metadataType, @NotNull BeanFactory<M> beanFactory);

    @Nullable
    @SuppressWarnings("unchecked")
    default <T> Bean<T> getBean(@NotNull String beanName) {
        return (Bean<T>) getBean(beanName, Object.class);
    }

    @Nullable
    <T> Bean<T> getBean(@NotNull String beanName, @NotNull Class<T> beanType);

    @Nullable
    <T> Bean<T> getBean(@NotNull Class<T> beanType);

    @NotNull
    <T> List<Bean<T>> getBeans(@NotNull Class<T> beanType);
}
