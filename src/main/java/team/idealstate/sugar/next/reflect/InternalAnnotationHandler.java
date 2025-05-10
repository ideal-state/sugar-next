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

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import team.idealstate.sugar.next.reflect.exception.ReflectionException;

class InternalAnnotationHandler implements ReflectionInvocationHandler {

    private final Map<String, Object> mappings;
    private final Map<String, Object> cache = new ConcurrentHashMap<>(16, 0.6F);

    InternalAnnotationHandler(Map<String, Object> mappings) {
        this.mappings = mappings;
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object ret = ReflectionInvocationHandler.super.invoke(proxy, method, args);
        if (ret != null) {
            return ret;
        }
        String methodName = method.getName();
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
        CHECK_RETURN_TYPE:
        if (!returnType.isAssignableFrom(valueType)) {
            THROW_EX:
            if (returnType.isArray() && valueType.isArray()) {
                Class<?> componentType = returnType.getComponentType();
                if (!componentType.isAssignableFrom(valueType.getComponentType())) {
                    int length = Array.getLength(value);
                    for (int i = 0; i < length; i++) {
                        Object arrValue = Array.get(value, i);
                        if (!componentType.isInstance(arrValue)) {
                            break THROW_EX;
                        }
                    }
                    Object arr = Array.newInstance(componentType, length);
                    System.arraycopy(value, 0, arr, 0, length);
                    value = arr;
                }
                break CHECK_RETURN_TYPE;
            }
            throw new ReflectionException(
                    "Return type of method " + methodName + " must be assignable from mappings value.");
        }

        cache.put(methodName, value);
        return value;
    }
}
