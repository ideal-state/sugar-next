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
import team.idealstate.minecraft.next.common.function.Lazy;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

final class SimpleBean<M extends Annotation, T> implements Bean<M, T> {

    private final M metadata;
    private final Lazy<T> lazy;
    private final boolean initialized;

    SimpleBean(@NotNull M metadata, @NotNull T instance) {
        Validation.notNull(metadata, "metadata must not be null.");
        Validation.notNull(instance, "instance must not be null.");
        this.metadata = metadata;
        this.lazy = Lazy.of(instance);
        this.initialized = true;
    }

    SimpleBean(@NotNull M metadata, @NotNull Lazy<T> lazy) {
        Validation.notNull(metadata, "metadata must not be null.");
        Validation.notNull(lazy, "lazy must not be null.");
        this.metadata = metadata;
        this.lazy = lazy;
        this.initialized = false;
    }

    @NotNull @Override
    public M getMetadata() {
        return metadata;
    }

    @Override
    @NotNull public T getInstance() {
        return Validation.requireNotNull(lazy.get(), "instance must not be null.");
    }

    public boolean isInitialized() {
        return initialized || lazy.isInitialized();
    }
}
