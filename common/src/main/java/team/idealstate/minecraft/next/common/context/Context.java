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
import team.idealstate.minecraft.next.common.eventbus.EventBus;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;
import team.idealstate.minecraft.next.common.validate.annotation.Nullable;

/**
 * @see ContextException
 */
public interface Context {

    String RESOURCE_BUNDLED = "bundled:";

    @NotNull static Context of(
            @NotNull ContextHolder contextHolder,
            @NotNull ContextLifecycle contextLifecycle,
            @NotNull EventBus eventBus) {
        return new SimpleContext(contextHolder, contextLifecycle, eventBus);
    }

    @NotNull String getName();

    @NotNull String getVersion();

    @NotNull File getDataFolder();

    @Nullable InputStream getResource(@NotNull String path) throws IOException;

    boolean isActive();

    void initialize();

    void load();

    void enable();

    void disable();

    void destroy();

    @Nullable <M extends Annotation> InstanceFactory<M, ?> instanceFactoryBy(@NotNull Class<M> metadataClass);

    @Nullable <M extends Annotation, T> InstanceFactory<M, T> instanceFactoryBy(
            @NotNull Class<M> metadataClass, @NotNull Class<T> instanceClass);

    <M extends Annotation> void setInstanceFactory(
            @NotNull Class<M> metadataClass, InstanceFactory<M, ?> componentFactory);

    @Nullable <M extends Annotation> Component<M, ?> componentBy(@NotNull Class<M> metadataClass);

    @Nullable <M extends Annotation, T> Component<M, T> componentBy(
            @NotNull Class<M> metadataClass, @NotNull Class<T> instanceClass);

    @NotNull <M extends Annotation> List<Component<M, ?>> componentsBy(@NotNull Class<M> metadataClass);

    @NotNull <M extends Annotation, T> List<Component<M, T>> componentsBy(
            @NotNull Class<M> metadataClass, @NotNull Class<T> instanceClass);
}
