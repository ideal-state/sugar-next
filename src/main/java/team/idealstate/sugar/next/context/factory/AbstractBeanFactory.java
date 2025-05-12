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
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import team.idealstate.sugar.next.context.BeanFactory;
import team.idealstate.sugar.next.context.Context;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractBeanFactory<M extends Annotation> implements BeanFactory<M> {

    @NonNull
    private final Class<M> metadataType;

    @NotNull
    @Override
    public final Class<M> getMetadataType() {
        return metadataType;
    }

    protected abstract boolean doValidate(
            @NotNull Context context, @NotNull String beanName, @NotNull M metadata, @NotNull Class<?> marked);

    @Override
    public final boolean validate(
            @NotNull Context context, @NotNull String beanName, @NotNull M metadata, @NotNull Class<?> marked) {
        Validation.notNull(context, "Context must not be null.");
        Validation.notNullOrBlank(beanName, "beanName must not be null or blank.");
        Validation.notNull(metadata, "Metadata must not be null.");
        Validation.notNull(marked, "Marked must not be null.");
        Class<M> metadataType = getMetadataType();
        Validation.is(
                metadataType.isInstance(metadata),
                String.format("metadata '%s' must be an instance of metadataType '%s'.", metadata, metadataType));
        return doValidate(context, beanName, metadata, marked);
    }

    @NotNull
    protected abstract <T> T doCreate(
            @NotNull Context context, @NotNull String beanName, @NotNull M metadata, @NotNull Class<T> marked);

    @NotNull
    @Override
    public final <T> T create(
            @NotNull Context context, @NotNull String beanName, @NotNull M metadata, @NotNull Class<T> marked) {
        Validation.is(validate(context, beanName, metadata, marked), "instance cannot be created.");
        T instance = doCreate(context, beanName, metadata, marked);
        Validation.notNull(instance, "Instance must not be null.");
        Validation.is(
                marked.isInstance(instance),
                String.format("Instance '%s' must be an instance of marked '%s'.", instance.getClass(), marked));
        return instance;
    }

    @NotNull
    protected <T> T doProxy(
            @NotNull Context context,
            @NotNull String beanName,
            @NotNull M metadata,
            @NotNull T instance,
            @NotNull Class<T> marked) {
        return instance;
    }

    @NotNull
    @Override
    public final <T> T proxy(
            @NotNull Context context,
            @NotNull String beanName,
            @NotNull M metadata,
            @NotNull T instance,
            @NotNull Class<T> marked) {
        Validation.notNull(context, "Context must not be null.");
        Validation.notNullOrBlank(beanName, "beanName must not be null or blank.");
        Validation.notNull(metadata, "Metadata must not be null.");
        Validation.notNull(instance, "Instance must not be null.");
        Validation.notNull(marked, "Marked must not be null.");
        Validation.is(
                marked.isInstance(instance),
                String.format("Instance '%s' must be an instance of marked '%s'.", instance.getClass(), marked));
        T proxy = doProxy(context, beanName, metadata, instance, marked);
        Validation.notNull(proxy, "Proxy must not be null.");
        Validation.is(
                marked.isInstance(proxy),
                String.format("Proxy '%s' must be an instance of marked '%s'.", proxy.getClass(), marked));
        return proxy;
    }
}
