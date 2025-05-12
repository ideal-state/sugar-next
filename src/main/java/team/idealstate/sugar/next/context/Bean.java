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
import team.idealstate.sugar.next.context.annotation.feature.DependsOn;
import team.idealstate.sugar.next.context.annotation.feature.Scope;
import team.idealstate.sugar.validate.annotation.NotNull;
import team.idealstate.sugar.validate.annotation.Nullable;

public interface Bean<T> {

    @NotNull
    String getName();

    @NotNull
    Scope getScope();

    @Nullable
    DependsOn getDependsOn();

    /**
     * @return 注意：此值不一定满足 <code>equals({@link #getMetadata()}.annotationType())<code/>
     */
    @NotNull
    Class<? extends Annotation> getMetadataType();

    /**
     * @return 注意：此值不一定满足 <code>annotationType().equals({@link #getMetadataType()})<code/>
     */
    @NotNull
    Annotation getMetadata();

    @NotNull
    Class<T> getMarked();

    @NotNull
    T getInstance();
}
