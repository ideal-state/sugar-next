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

package team.idealstate.sugar.next.boot.mybatis.factory;

import static team.idealstate.sugar.next.function.Functional.functional;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationTargetException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatchers;
import team.idealstate.sugar.next.context.Context;
import team.idealstate.sugar.next.context.exception.ContextException;
import team.idealstate.sugar.next.context.factory.AbstractBeanFactory;
import team.idealstate.sugar.next.database.TransactionManager;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;

public abstract class AbstractMapperBeanFactory<M extends Annotation> extends AbstractBeanFactory<M> {
    protected AbstractMapperBeanFactory(@NonNull Class<M> metadataType) {
        super(metadataType);
    }

    @Override
    protected boolean doValidate(
            @NotNull Context context, @NotNull String beanName, @NotNull M metadata, @NotNull Class<?> marked) {
        return marked.isInterface();
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    protected <T> T doCreate(
            @NotNull Context context, @NotNull String beanName, @NotNull M metadata, @NotNull Class<T> marked) {
        TransactionManager transactionManager = TransactionComponentBeanFactory.getTransactionManager(context);
        Class<?> proxyType = functional(new ByteBuddy()
                        .subclass(marked)
                        .method(ElementMatchers.isPublic().and(ElementMatchers.not(ElementMatchers.isStatic())))
                        .intercept(MethodDelegation.to(new MapperInterceptor(transactionManager, marked)))
                        .make())
                .use(Class.class, unloaded -> unloaded.load(context.getClassLoader())
                        .getLoaded());
        try {
            return (T) proxyType.getConstructor().newInstance();
        } catch (InstantiationException
                | IllegalAccessException
                | InvocationTargetException
                | NoSuchMethodException e) {
            throw new ContextException(e);
        }
    }

    @RequiredArgsConstructor
    protected static final class MapperInterceptor {
        @NonNull
        private final TransactionManager transactionManager;

        @NonNull
        private final Class<?> marked;

        @RuntimeType
        @SuppressWarnings("unused")
        public Object intercept(@Origin MethodHandle methodHandle, @AllArguments Object[] arguments) throws Throwable {
            Object repository = transactionManager.getRepository(marked);
            Validation.notNull(repository, "repository must not be null.");
            return methodHandle.invoke(repository, arguments);
        }
    }
}
