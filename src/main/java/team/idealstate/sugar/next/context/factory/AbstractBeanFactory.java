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
import lombok.RequiredArgsConstructor;
import team.idealstate.sugar.next.context.BeanFactory;
import team.idealstate.sugar.next.context.Context;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractBeanFactory<M extends Annotation> implements BeanFactory<M> {

    @NotNull
    private final Class<M> metadataType;

    @NotNull
    @Override
    public final Class<M> getMetadataType() {
        return metadataType;
    }

    protected abstract boolean doValidate(
            @NotNull Context context, @NotNull String beanName, @NotNull M metadata, @NotNull Class<?> beanType);

    @Override
    public final boolean validate(
            @NotNull Context context, @NotNull String beanName, @NotNull M metadata, @NotNull Class<?> beanType) {
        Validation.notNull(context, "Context must not be null.");
        Validation.notNullOrBlank(beanName, "Bean name must not be null or blank.");
        Validation.notNull(metadata, "Metadata must not be null.");
        Validation.notNull(beanType, "BeanType must not be null.");
        Class<M> metadataType = getMetadataType();
        Validation.is(
                metadataType.isInstance(metadata),
                String.format("Metadata '%s' must be an instance of metadataType '%s'.", metadata, metadataType));
        return doValidate(context, beanName, metadata, beanType);
    }

    @NotNull
    protected abstract <T> T doCreate(
            @NotNull Context context, @NotNull String beanName, @NotNull M metadata, @NotNull Class<T> beanType);

    @NotNull
    @Override
    public final <T> T create(
            @NotNull Context context, @NotNull String beanName, @NotNull M metadata, @NotNull Class<T> beanType) {
        Validation.is(
                validate(context, beanName, metadata, beanType),
                String.format("Instance of '%s' cannot be created.", beanType.getName()));
        T instance = doCreate(context, beanName, metadata, beanType);
        Validation.notNull(instance, "Instance must not be null.");
        Validation.is(
                beanType.isInstance(instance),
                String.format("Instance '%s' must be an instance of beanType '%s'.", instance.getClass(), beanType));
        return instance;
    }

    @NotNull
    protected <T> T doProxy(
            @NotNull Context context,
            @NotNull String beanName,
            @NotNull M metadata,
            @NotNull T instance,
            @NotNull Class<T> beanType) {
        return instance;
    }

    @NotNull
    @Override
    public final <T> T proxy(
            @NotNull Context context,
            @NotNull String beanName,
            @NotNull M metadata,
            @NotNull T instance,
            @NotNull Class<T> beanType) {
        Validation.notNull(context, "Context must not be null.");
        Validation.notNullOrBlank(beanName, "beanName must not be null or blank.");
        Validation.notNull(metadata, "Metadata must not be null.");
        Validation.notNull(instance, "Instance must not be null.");
        Validation.notNull(beanType, "BeanType must not be null.");
        Validation.is(
                beanType.isInstance(instance),
                String.format("Instance '%s' must be an instance of beanType '%s'.", instance.getClass(), beanType));
        T proxy = doProxy(context, beanName, metadata, instance, beanType);
        Validation.notNull(proxy, "Proxy must not be null.");
        Validation.is(
                beanType.isInstance(proxy),
                String.format("Proxy '%s' must be an instance of beanType '%s'.", proxy.getClass(), beanType));
        return proxy;
    }
}
