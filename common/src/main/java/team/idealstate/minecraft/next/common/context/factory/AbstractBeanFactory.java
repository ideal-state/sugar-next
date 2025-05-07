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

package team.idealstate.minecraft.next.common.context.factory;

import java.lang.annotation.Annotation;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import team.idealstate.minecraft.next.common.context.Context;
import team.idealstate.minecraft.next.common.context.BeanFactory;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractBeanFactory<M extends Annotation, T>
        implements BeanFactory<M, T> {

    @NonNull private final Class<M> metadataType;
    @NonNull private final Class<T> instanceType;

    @NotNull @Override
    public final Class<M> getMetadataType() {
        return metadataType;
    }

    @NotNull @Override
    public final Class<T> getInstanceType() {
        return instanceType;
    }

    protected abstract boolean doCanBeCreated(
            @NotNull Context context, @NotNull M metadata, @NotNull Class<?> marked);

    @Override
    public final boolean canBeCreated(
            @NotNull Context context, @NotNull M metadata, @NotNull Class<?> marked) {
        Validation.notNull(context, "context must not be null.");
        Validation.notNull(metadata, "metadata must not be null.");
        Validation.notNull(marked, "marked must not be null.");
        Class<M> metadataType = getMetadataType();
        Validation.is(
                metadataType.isInstance(metadata),
                String.format("metadata '%s' must be an instance of metadataType '%s'.", metadata, metadataType));
        return doCanBeCreated(context, metadata, marked);
    }

    @NotNull protected abstract T doCreate(
            @NotNull Context context, @NotNull M metadata, @NotNull Class<?> marked);

    @NotNull @Override
    public final T create(@NotNull Context context, @NotNull M metadata, @NotNull Class<?> marked) {
        Validation.is(canBeCreated(context, metadata, marked), "instance cannot be created.");
        T instance = doCreate(context, metadata, marked);
        Class<T> instanceType = getInstanceType();
        Validation.is(
                instanceType.isInstance(instance),
                String.format("instance '%s' must be an instance of instanceType '%s'.", instance, instanceType));
        return instance;
    }
}
