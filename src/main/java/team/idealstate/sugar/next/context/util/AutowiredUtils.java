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

package team.idealstate.sugar.next.context.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import team.idealstate.sugar.logging.Log;
import team.idealstate.sugar.next.context.Bean;
import team.idealstate.sugar.next.context.Context;
import team.idealstate.sugar.next.context.annotation.feature.Autowired;
import team.idealstate.sugar.next.context.annotation.feature.Qualifier;
import team.idealstate.sugar.next.context.exception.ContextException;
import team.idealstate.sugar.next.function.Lazy;
import team.idealstate.sugar.string.StringUtils;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;
import team.idealstate.sugar.validate.annotation.Nullable;

public abstract class AutowiredUtils {

    @Nullable
    public static Object autowire(@NotNull Context context, @NotNull Class<?> instanceType) {
        Constructor<?> constructor = null;
        for (Constructor<?> c : instanceType.getConstructors()) {
            if (c.isAnnotationPresent(Autowired.class)) {
                constructor = c;
                break;
            }
        }
        if (constructor != null) {
            return AutowiredUtils.autowire(context, instanceType, constructor);
        }
        return null;
    }

    @NotNull
    public static Object autowire(
            @NotNull Context context, @NotNull Class<?> instanceType, @NotNull Constructor<?> constructor) {
        return Validation.requireNotNull(
                autowire(context, null, instanceType, constructor), "Autowired constructed result must not be null.");
    }

    @Nullable
    public static Object autowire(
            @NotNull Context context,
            @NotNull Object instance,
            @NotNull Class<?> instanceType,
            @NotNull Method method) {
        return autowire(context, instance, instanceType, (Executable) method);
    }

    @SuppressWarnings({"unchecked", "rawtypes", "StatementWithEmptyBody"})
    private static Object autowire(
            @NotNull Context context, Object instance, @NotNull Class<?> instanceType, @NotNull Executable executable) {
        Validation.notNull(context, "Context must not be null.");
        Validation.notNull(instanceType, "Instance type must not be null.");
        Validation.notNull(executable, "Executable must not be null.");
        if (executable instanceof Constructor) {

        } else if (executable instanceof Method) {
            Validation.notNull(instance, "Instance must not be null.");
        } else {
            return null;
        }
        if (!executable.isAnnotationPresent(Autowired.class)) {
            return null;
        }
        String executableName = executable.getName();
        String instanceTypeName = instanceType.getName();
        if (Modifier.isStatic(executable.getModifiers())) {
            Log.warn(String.format(
                    "Autowire: '%s' static executable '%s' is ignored.", instanceTypeName, executableName));
            return null;
        }
        long start = System.currentTimeMillis();
        Parameter[] parameters = executable.getParameters();
        Object[] parameterValues = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String parameterName = parameter.getName();
            Class<?> parameterType = parameter.getType();
            Class<?> autowireType =
                    getAutowireType(instanceTypeName, executableName, parameterName, parameter.getParameterizedType());
            Object value = null;
            Qualifier qualifier = parameter.getAnnotation(Qualifier.class);
            if (Bean.class.equals(parameterType)) {
                List<Bean<?>> autowireValue =
                        getAutowireValue(context, qualifier, parameterName, (Class) autowireType, true);
                if (!autowireValue.isEmpty()) {
                    value = autowireValue.get(0);
                }
            } else if (Lazy.class.equals(parameterType)) {
                List<Bean<?>> autowireValue =
                        getAutowireValue(context, qualifier, parameterName, (Class) autowireType, true);
                if (!autowireValue.isEmpty()) {
                    Bean<?> bean = autowireValue.get(0);
                    value = Lazy.of(bean::getInstance);
                }
            } else if (List.class.equals(parameterType)) {
                List<Bean<?>> autowireValue =
                        getAutowireValue(context, qualifier, parameterName, (Class) autowireType, false);
                if (autowireValue.isEmpty()) {
                    value = Collections.emptyList();
                } else {
                    value = autowireValue.stream().map(Bean::getInstance).collect(Collectors.toList());
                }
            } else if (Map.class.equals(parameterType)) {
                List<Bean<?>> autowireValue =
                        getAutowireValue(context, qualifier, parameterName, (Class) autowireType, false);
                if (autowireValue.isEmpty()) {
                    value = Collections.emptyMap();
                } else {
                    value = autowireValue.stream().collect(Collectors.toMap(Bean::getName, Bean::getInstance));
                }
            } else {
                List<Bean<?>> autowireValue =
                        getAutowireValue(context, qualifier, parameterName, (Class) autowireType, true);
                if (!autowireValue.isEmpty()) {
                    value = autowireValue.get(0).getInstance();
                }
            }
            if (parameter.isAnnotationPresent(NotNull.class)) {
                Validation.notNull(
                        value,
                        String.format(
                                "Autowire: '%s' executable '%s' parameter '%s' value must not be null.",
                                instanceTypeName, executableName, parameterName));
            }
            parameterValues[i] = value;
        }
        executable.setAccessible(true);
        Object result;
        try {
            Log.debug(() -> String.format("Autowire: '%s' executable '%s'...", instanceTypeName, executableName));
            if (executable instanceof Constructor) {
                result = ((Constructor) executable).newInstance(parameterValues);
            } else {
                result = ((Method) executable).invoke(instance, parameterValues);
            }
            Log.debug(() -> String.format(
                    "(%s) Autowire: '%s' executable '%s' done.",
                    System.currentTimeMillis() - start, instanceTypeName, executableName));
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new ContextException(e);
        }
        return result;
    }

    @NotNull
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> List<Bean<T>> getAutowireValue(
            @NotNull Context context,
            @Nullable Qualifier qualifier,
            @NotNull String parameterName,
            @NotNull Class<T> autowireType,
            boolean one) {
        if (one || qualifier != null) {
            Bean bean;
            if (qualifier == null) {
                bean = context.getBean(autowireType);
            } else {
                String beanName = qualifier.value();
                bean = context.getBean(StringUtils.isNullOrBlank(beanName) ? parameterName : beanName, autowireType);
            }
            return bean == null ? Collections.emptyList() : Collections.singletonList(bean);
        }
        return context.getBeans(autowireType);
    }

    @NotNull
    private static Class<?> getAutowireType(
            @NotNull String instanceTypeName,
            @NotNull String executableName,
            @NotNull String parameterName,
            @NotNull Type parameterType) {
        Type autowireType = null;
        if (parameterType instanceof Class<?>) {
            autowireType = parameterType;
            Validation.is(
                    !((Class<?>) autowireType).isArray() && ((Class<?>) autowireType).getTypeParameters().length == 0,
                    String.format(
                            "Autowire: '%s' executable '%s' parameter '%s' must not an array or raw generic type.",
                            instanceTypeName, executableName, parameterName));
        } else if (parameterType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) parameterType;
            Class<?> rawType = ((Class<?>) parameterizedType.getRawType());
            if (Bean.class.equals(rawType) || Lazy.class.equals(rawType) || List.class.equals(rawType)) {
                autowireType = parameterizedType.getActualTypeArguments()[0];
            } else if (Map.class.equals(rawType)) {
                Type[] actualTypes = parameterizedType.getActualTypeArguments();
                Validation.is(
                        ((Class<?>) actualTypes[0]).isAssignableFrom(String.class),
                        String.format(
                                "Autowire: '%s' executable '%s' parameter '%s' key type must be assignable from String.",
                                instanceTypeName, executableName, parameterName));
                autowireType = actualTypes[1];
            }
        }
        Validation.is(
                isNonArrayNonGenericClass(autowireType),
                String.format(
                        "Autowire: '%s' executable '%s' parameter '%s' must not be an array or generic type.",
                        instanceTypeName, executableName, parameterName));
        assert autowireType != null;
        return (Class<?>) autowireType;
    }

    private static boolean isNonArrayNonGenericClass(@Nullable Type type) {
        if (!(type instanceof Class)) {
            return false;
        }
        Class<?> cls = (Class<?>) type;
        return !cls.isArray() && cls.getTypeParameters().length == 0;
    }
}
