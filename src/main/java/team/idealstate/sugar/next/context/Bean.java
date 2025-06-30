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
import java.util.List;
import team.idealstate.sugar.next.context.annotation.component.Component;
import team.idealstate.sugar.next.context.annotation.feature.DependsOn;
import team.idealstate.sugar.next.context.annotation.feature.Scope;
import team.idealstate.sugar.validate.annotation.NotNull;
import team.idealstate.sugar.validate.annotation.Nullable;

public interface Bean<T> {

    @NotNull
    Context getContext();

    @NotNull
    String getName();

    @NotNull
    Scope getScope();

    /** @deprecated 不完整的依赖项数据，请使用 {@link #getDependencies()} 代替 */
    @Deprecated
    @Nullable
    default DependsOn getDependsOn() {
        List<DependsOn> dependencies = getDependencies();
        return dependencies.isEmpty() ? null : dependencies.get(0);
    }

    @NotNull
    List<DependsOn> getDependencies();

    /** @deprecated 过时的内容，请使用 {@link #getMetadata()}.getClass() 代替 */
    @Deprecated
    @NotNull
    default Class<? extends Annotation> getMetadataType() {
        return getMetadata().getClass();
    }

    /** @return 当前 Bean 的元数据（构造时）， 此值不一定与 {@link #getActualMetadata()} 相等， 因为它有可能被委托成 {@link Component} */
    @NotNull
    Annotation getMetadata();

    /** @return 当前 Bean 的实际元数据（编译时），此值不一定与 {@link #getMetadata()} 相等 */
    @NotNull
    Annotation getActualMetadata();

    @NotNull
    Class<T> getType();

    /** @deprecated 意义不明确的命名，请使用 {@link #getType()} 代替 */
    @Deprecated
    @NotNull
    default Class<T> getMarked() {
        return getType();
    }

    @NotNull
    T getInstance();
}
