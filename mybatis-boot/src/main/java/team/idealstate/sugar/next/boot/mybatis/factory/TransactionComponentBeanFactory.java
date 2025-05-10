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

import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatchers;
import team.idealstate.sugar.logging.Log;
import team.idealstate.sugar.next.boot.mybatis.MyBatis;
import team.idealstate.sugar.next.context.Bean;
import team.idealstate.sugar.next.context.Context;
import team.idealstate.sugar.next.context.annotation.component.Component;
import team.idealstate.sugar.next.context.exception.ContextException;
import team.idealstate.sugar.next.context.factory.ComponentBeanFactory;
import team.idealstate.sugar.next.database.TransactionManager;
import team.idealstate.sugar.next.database.annotation.Transaction;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;

public class TransactionComponentBeanFactory extends ComponentBeanFactory {

    @NotNull
    @Override
    @SuppressWarnings({"unchecked"})
    protected <T> T doProxy(
            @NotNull Context context, @NotNull String beanName, @NotNull Component metadata, @NotNull T instance) {
        Class<?> instanceType = instance.getClass();
        Method[] methods = instanceType.getMethods();
        if (methods.length == 0) {
            return instance;
        }
        Map<String, Transaction> transactions = new HashMap<>(methods.length);
        for (Method method : methods) {
            Transaction transaction = method.getAnnotation(Transaction.class);
            if (transaction == null) {
                continue;
            }
            if (Modifier.isStatic(method.getModifiers())) {
                Log.warn(String.format(
                        "%s: '%s' static method '%s' is ignored.",
                        getClass().getSimpleName(), instanceType.getName(), method.getName()));
                continue;
            }
            transactions.put(method.toString(), transaction);
        }
        if (transactions.isEmpty()) {
            return instance;
        }
        TransactionManager transactionManager = getTransactionManager(context);
        transactions = Collections.unmodifiableMap(transactions);
        Class<?> proxyType = functional(new ByteBuddy()
                        .subclass(instanceType)
                        .method(ElementMatchers.any().and(ElementMatchers.not(ElementMatchers.isStatic())))
                        .intercept(MethodDelegation.to(
                                new TransactionInterceptor(transactionManager, instance, transactions)))
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

    @NotNull
    static TransactionManager getTransactionManager(@NotNull Context context) {
        List<Bean<TransactionManager>> beans = context.getBeans(TransactionManager.class);
        Validation.is(beans.size() == 1, "there are multiple transaction managers in the current context.");
        Bean<TransactionManager> bean = beans.get(0);
        Validation.is(MyBatis.class.isAssignableFrom(bean.getMarked()), "transaction manager must be MyBatis.");
        return bean.getInstance();
    }

    @RequiredArgsConstructor
    protected static final class TransactionInterceptor {
        @NonNull
        private final TransactionManager transactionManager;

        @NonNull
        private final Object instance;

        @NonNull
        private final Map<String, Transaction> transactions;

        @RuntimeType
        @SuppressWarnings("unused")
        public Object intercept(
                @Origin String method, @Origin MethodHandle methodHandle, @AllArguments Object[] arguments)
                throws Throwable {
            Transaction transaction = transactions.get(method);
            MethodHandle bound = methodHandle.bindTo(instance);
            if (transaction == null) {
                return bound.invokeWithArguments(arguments);
            }
            return functional(transactionManager.openTransaction(
                            transaction.executionMode(), transaction.isolationLevel()))
                    .use(Object.class, session -> {
                        try {
                            return bound.invokeWithArguments(arguments);
                        } catch (Throwable e) {
                            session.rollback();
                            throw e;
                        }
                    });
        }
    }
}
