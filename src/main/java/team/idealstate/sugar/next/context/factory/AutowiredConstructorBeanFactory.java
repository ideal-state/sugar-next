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

package team.idealstate.sugar.next.context.factory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import team.idealstate.sugar.next.context.Context;
import team.idealstate.sugar.next.context.annotation.feature.Autowired;
import team.idealstate.sugar.next.context.exception.ContextException;
import team.idealstate.sugar.next.context.util.AutowiredUtils;
import team.idealstate.sugar.validate.annotation.NotNull;

public abstract class AutowiredConstructorBeanFactory<M extends Annotation> extends NoArgsConstructorBeanFactory<M> {

    protected AutowiredConstructorBeanFactory(@NotNull Class<M> metadataClass) {
        super(metadataClass);
    }

    @Override
    protected boolean doValidate(
            @NotNull Context context, @NotNull String beanName, @NotNull M metadata, @NotNull Class<?> marked) {
        int found = 0;
        for (Constructor<?> constructor : marked.getConstructors()) {
            if (constructor.isAnnotationPresent(Autowired.class)) {
                found++;
            }
        }
        if (found > 1) {
            throw new ContextException(String.format(
                    "%s: %s has more than one constructor annotated with @Autowired.",
                    getMetadataType().getSimpleName(), marked.getName()));
        }
        if (found > 0) {
            return true;
        }
        return super.doValidate(context, beanName, metadata, marked);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    protected <T> T doCreate(
            @NotNull Context context, @NotNull String beanName, @NotNull M metadata, @NotNull Class<T> marked) {
        Constructor<?> constructor = null;
        for (Constructor<?> c : marked.getConstructors()) {
            if (c.isAnnotationPresent(Autowired.class)) {
                constructor = c;
                break;
            }
        }
        if (constructor != null) {
            return (T) AutowiredUtils.autowire(context, marked, constructor);
        }
        return super.doCreate(context, beanName, metadata, marked);
    }
}
