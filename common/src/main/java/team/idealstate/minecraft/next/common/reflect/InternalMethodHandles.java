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

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import team.idealstate.minecraft.next.common.reflect.exception.ReflectionException;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

@SuppressWarnings("JavaReflectionMemberAccess")
abstract class InternalMethodHandles {

    private static final int ALLOWED_MODES =
            MethodHandles.Lookup.PRIVATE
                    | MethodHandles.Lookup.PROTECTED
                    | MethodHandles.Lookup.PACKAGE
                    | MethodHandles.Lookup.PUBLIC;
    private static final Constructor<MethodHandles.Lookup> JDK_8_LOOKUP_CONSTRUCTOR;

    static {
        try {
            JDK_8_LOOKUP_CONSTRUCTOR =
                    MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
        } catch (NoSuchMethodException e) {
            throw new ReflectionException(e);
        }
        JDK_8_LOOKUP_CONSTRUCTOR.setAccessible(true);
    }

    @NotNull public static MethodHandles.Lookup publicLookup() {
        return MethodHandles.publicLookup();
    }

    @NotNull public static MethodHandles.Lookup lookup() {
        return MethodHandles.lookup();
    }

    @NotNull public static MethodHandles.Lookup privateLookup(@NotNull Class<?> lookupClass) {
        try {
            return JDK_8_LOOKUP_CONSTRUCTOR.newInstance(lookupClass, ALLOWED_MODES);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new ReflectionException(e);
        }
    }
}
