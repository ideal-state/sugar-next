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

package team.idealstate.minecraft.next.common.reflect;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public interface ReflectionInvocationHandler extends InvocationHandler {

    String HASH_CODE = "hashCode";
    String EQUALS = "equals";
    String TO_STRING = "toString";

    @Override
    default Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object ret = invokeHashCode(proxy, method, args);
        if (ret == null) {
            ret = invokeEquals(proxy, method, args);
            if (ret == null) {
                ret = invokeToString(proxy, method, args);
            }
        }
        return ret;
    }

    default Object invokeHashCode(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if (HASH_CODE.equals(methodName)
                && (args == null || args.length == 0)
                && !Modifier.isStatic(method.getModifiers())
                && int.class.equals(method.getReturnType())) {
            return System.identityHashCode(proxy);
        }
        return null;
    }

    default Object invokeEquals(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if (EQUALS.equals(methodName)
                && (args != null && args.length == 1)
                && !Modifier.isStatic(method.getModifiers())
                && Object.class.equals(method.getParameterTypes()[0])
                && boolean.class.equals(method.getReturnType())) {
            return proxy == args[0];
        }
        return null;
    }

    default Object invokeToString(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if (TO_STRING.equals(methodName)
                && (args == null || args.length == 0)
                && !Modifier.isStatic(method.getModifiers())
                && String.class.equals(method.getReturnType())) {
            return proxy.getClass().getName()
                    + "@"
                    + Integer.toHexString(System.identityHashCode(proxy));
        }
        return null;
    }
}
