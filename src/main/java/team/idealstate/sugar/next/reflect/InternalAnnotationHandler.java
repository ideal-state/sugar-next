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

package team.idealstate.sugar.next.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import team.idealstate.sugar.next.lang.WrappedType;
import team.idealstate.sugar.next.reflect.exception.ReflectionException;
import team.idealstate.sugar.validate.annotation.NotNull;

class InternalAnnotationHandler implements ReflectionInvocationHandler {

    private final Class<? extends Annotation> annotationType;
    private final Map<String, Object> mappings;
    private final Map<String, Object> cache = new ConcurrentHashMap<>(16, 0.6F);

    InternalAnnotationHandler(Class<? extends Annotation> annotationType, Map<String, Object> mappings) {
        this.annotationType = annotationType;
        this.mappings = mappings;
    }

    private static <T> T assignable(@NotNull Class<T> type, @NotNull Object value) {
        Class<?> valueType = value.getClass();
        boolean assignableFrom = type.isAssignableFrom(valueType);
        if (!assignableFrom) {
            try {
                return WrappedType.of(type).unwrap(value);
            } catch (Throwable e) {
                throw new ReflectionException(
                        String.format("Value '%s' cannot be cast to type '%s'.", value, type.getName()), e);
            }
        }
        return type.cast(value);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object ret = ReflectionInvocationHandler.super.invoke(proxy, method, args);
        if (ret != null) {
            return ret;
        }
        String methodName = method.getName();
        if ("annotationType".equals(methodName)) {
            return annotationType;
        }
        if (cache.containsKey(methodName)) {
            return cache.get(methodName);
        }
        Object value = mappings.get(methodName);
        if (value == null) {
            value = method.getDefaultValue();
            if (value == null) {
                throw new ReflectionException("Default value for method " + methodName + " is null.");
            }
        }

        Class<?> returnType = method.getReturnType();
        Class<?> valueType = value.getClass();
        try {
            if (returnType.isArray() && valueType.isArray()) {
                Class<?> componentType = returnType.getComponentType();
                int length = Array.getLength(value);
                Object array = Array.newInstance(componentType, length);
                for (int i = 0; i < length; i++) {
                    Array.set(array, i, assignable(componentType, Array.get(value, i)));
                }
                value = array;
            } else {
                value = assignable(returnType, value);
            }
        } catch (ReflectionException e) {
            throw new ReflectionException(
                    String.format(
                            "Return type '%s' of method '%s' must be assignable from mappings value type '%s'.",
                            returnType.getName(), methodName, valueType.getName()),
                    e);
        }

        cache.put(methodName, value);
        return value;
    }
}
