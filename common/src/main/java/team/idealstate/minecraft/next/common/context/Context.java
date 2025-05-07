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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.List;
import team.idealstate.minecraft.next.common.context.exception.ContextException;
import team.idealstate.minecraft.next.common.database.DatabaseSessionFactory;
import team.idealstate.minecraft.next.common.eventbus.EventBus;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;
import team.idealstate.minecraft.next.common.validate.annotation.Nullable;

/**
 * @see ContextException
 */
public interface Context {

    String RESOURCE_BUNDLED = "bundled:";
    String PROPERTY_ENVIRONMENT_KEY = "minecraftnext.environment";

    @NotNull static Context of(
            @NotNull ContextHolder contextHolder,
            @NotNull ContextLifecycle contextLifecycle,
            @NotNull EventBus eventBus) {
        return new SimpleContext(contextHolder, contextLifecycle, eventBus);
    }

    /**
     * @return 上下文所处的环境
     * @see #PROPERTY_ENVIRONMENT_KEY
     */
    @NotNull String getEnvironment();

    @NotNull ClassLoader getClassLoader();

    @NotNull String getName();

    @NotNull String getVersion();

    @NotNull File getDataFolder();

    @Nullable InputStream getResource(@NotNull String path) throws IOException;

    @Nullable
    DatabaseSessionFactory getDatabaseSessionFactory();

    boolean isActive();

    void initialize();

    void load();

    void enable();

    void disable();

    void destroy();

    @Nullable <M extends Annotation> BeanFactory<M, ?> getBeanFactory(@NotNull Class<M> metadataType);

    @Nullable <M extends Annotation, T> BeanFactory<M, T> getBeanFactory(
            @NotNull Class<M> metadataType, @NotNull Class<T> beanType);

    <M extends Annotation> void setBeanFactory(
            @NotNull Class<M> metadataType, @Nullable BeanFactory<M, ?> beanFactory);

    @Nullable <M extends Annotation> Bean<M, ?> getBean(@NotNull Class<M> metadataType);

    @Nullable <M extends Annotation, T> Bean<M, T> getBean(
            @NotNull Class<M> metadataType, @NotNull Class<T> beanType);

    @NotNull <M extends Annotation> List<Bean<M, ?>> getBeans(@NotNull Class<M> metadataType);

    @NotNull <M extends Annotation, T> List<Bean<M, T>> getBeans(
            @NotNull Class<M> metadataType, @NotNull Class<T> beanType);
}
