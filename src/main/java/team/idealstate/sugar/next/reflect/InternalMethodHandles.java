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

import team.idealstate.sugar.next.reflect.exception.ReflectionException;
import team.idealstate.sugar.validate.annotation.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

@SuppressWarnings("JavaReflectionMemberAccess")
abstract class InternalMethodHandles {

    private static final int ALLOWED_MODES = MethodHandles.Lookup.PRIVATE
            | MethodHandles.Lookup.PROTECTED
            | MethodHandles.Lookup.PACKAGE
            | MethodHandles.Lookup.PUBLIC;
    private static final MethodHandle JDK_9_PRIVATE_LOOKUP_CREATOR;
    private static final MethodHandle JDK_8_PRIVATE_LOOKUP_CREATOR;

    static {
        MethodHandle jdk8PrivateLookupCreator;
        MethodHandle jdk9PrivateLookupCreator;
        try {
            java.lang.reflect.Constructor<?> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
            constructor.setAccessible(true);
            jdk8PrivateLookupCreator = lookup().unreflectConstructor(constructor);
            jdk9PrivateLookupCreator = null;
        } catch (NoSuchMethodException e) {
            try {
                java.lang.reflect.Method method = MethodHandles.class.getDeclaredMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);
                jdk9PrivateLookupCreator = lookup().unreflect(method);
                jdk8PrivateLookupCreator = null;
            } catch (NoSuchMethodException | IllegalAccessException ex) {
                throw new ReflectionException(ex);
            }
        } catch (IllegalAccessException e) {
            throw new ReflectionException(e);
        }
        JDK_8_PRIVATE_LOOKUP_CREATOR = jdk8PrivateLookupCreator;
        JDK_9_PRIVATE_LOOKUP_CREATOR = jdk9PrivateLookupCreator;
    }

    @NotNull
    public static MethodHandles.Lookup publicLookup() {
        return MethodHandles.publicLookup();
    }

    @NotNull
    public static MethodHandles.Lookup lookup() {
        return MethodHandles.lookup();
    }

    @NotNull
    public static MethodHandles.Lookup privateLookup(@NotNull Class<?> lookupClass) {
        try {
            if (JDK_8_PRIVATE_LOOKUP_CREATOR == null) {
                return (MethodHandles.Lookup) JDK_9_PRIVATE_LOOKUP_CREATOR.invokeWithArguments(lookupClass, lookup());
            }
            return (MethodHandles.Lookup) JDK_8_PRIVATE_LOOKUP_CREATOR.invokeWithArguments(lookupClass, ALLOWED_MODES);
        } catch (Throwable e) {
            throw new ReflectionException(e);
        }
    }
}
