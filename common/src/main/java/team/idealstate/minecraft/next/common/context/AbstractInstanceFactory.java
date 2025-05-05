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

import java.lang.annotation.Annotation;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractInstanceFactory<M extends Annotation, T>
        implements InstanceFactory<M, T> {

    @NonNull private final Class<M> metadataClass;
    @NonNull private final Class<T> instanceClass;

    @NotNull @Override
    public final Class<M> getMetadataClass() {
        return metadataClass;
    }

    @NotNull @Override
    public final Class<T> getInstanceClass() {
        return instanceClass;
    }

    protected abstract boolean doCanBeCreated(
            @NotNull Context context, @NotNull M metadata, @NotNull Class<?> marked);

    @Override
    public final boolean canBeCreated(
            @NotNull Context context, @NotNull M metadata, @NotNull Class<?> marked) {
        Validation.notNull(context, "context must not be null.");
        Validation.notNull(metadata, "metadata must not be null.");
        Validation.notNull(marked, "marked must not be null.");
        Validation.vote(
                getMetadataClass().isInstance(metadata),
                "metadata must be an instance of metadataClass.");
        return doCanBeCreated(context, metadata, marked);
    }

    @NotNull protected abstract T doCreate(
            @NotNull Context context, @NotNull M metadata, @NotNull Class<?> marked);

    @NotNull @Override
    public final T create(@NotNull Context context, @NotNull M metadata, @NotNull Class<?> marked) {
        Validation.vote(canBeCreated(context, metadata, marked), "instance cannot be created.");
        T instance = doCreate(context, metadata, marked);
        Validation.vote(
                getInstanceClass().isInstance(instance),
                "instance must be an instance of instanceClass.");
        return instance;
    }
}
