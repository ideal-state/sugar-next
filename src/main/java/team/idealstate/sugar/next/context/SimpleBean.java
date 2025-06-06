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

import java.lang.annotation.Annotation;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import team.idealstate.sugar.next.context.annotation.feature.DependsOn;
import team.idealstate.sugar.next.context.annotation.feature.Scope;
import team.idealstate.sugar.next.function.closure.Provider;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;

@Data
final class SimpleBean<T> implements Bean<T> {

    @NonNull
    private final String name;

    @NonNull
    private final Scope scope;

    private final DependsOn dependsOn;

    @NonNull
    private final Class<? extends Annotation> metadataType;

    @NonNull
    private final Annotation metadata;

    @NonNull
    private final Class<T> marked;

    @NonNull
    @Getter(AccessLevel.PRIVATE)
    private final Provider<T> provider;

    @Override
    @NotNull
    public T getInstance() {
        return Validation.requireNotNull(getProvider().provide(), "instance must not be null.");
    }
}
