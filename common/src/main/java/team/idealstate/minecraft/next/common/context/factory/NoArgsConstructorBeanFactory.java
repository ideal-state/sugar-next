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
import team.idealstate.minecraft.next.common.context.Context;
import team.idealstate.minecraft.next.common.context.exception.ContextException;
import team.idealstate.minecraft.next.common.logging.Log;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

public abstract class NoArgsConstructorBeanFactory<M extends Annotation, T>
        extends AbstractBeanFactory<M, T> {
    protected NoArgsConstructorBeanFactory(
            @NotNull Class<M> metadataClass, @NotNull Class<T> instanceClass) {
        super(metadataClass, instanceClass);
    }

    @Override
    protected boolean doCanBeCreated(
            @NotNull Context context, @NotNull M metadata, @NotNull Class<?> marked) {
        if (!getInstanceType().isAssignableFrom(marked)) {
            Log.warn(
                    String.format(
                            "%s: %s is not assignable to %s.",
                            getMetadataType().getSimpleName(),
                            marked.getName(),
                            getInstanceType().getName()));
            return false;
        }
        try {
            marked.getConstructor();
        } catch (NoSuchMethodException e) {
            Log.warn(
                    String.format(
                            "%s: %s has no default constructor.",
                            getMetadataType().getSimpleName(), marked.getName()));
            return false;
        }
        return true;
    }

    @NotNull @Override
    @SuppressWarnings({"unchecked"})
    protected T doCreate(@NotNull Context context, @NotNull M metadata, @NotNull Class<?> marked) {
        try {
            return (T) marked.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new ContextException(e);
        }
    }
}
