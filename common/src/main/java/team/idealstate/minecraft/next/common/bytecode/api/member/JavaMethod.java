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

package team.idealstate.minecraft.next.common.bytecode.api.member;

import java.lang.reflect.Method;
import java.util.Arrays;
import team.idealstate.minecraft.next.common.bytecode.Java;
import team.idealstate.minecraft.next.common.bytecode.api.JavaAccessible;
import team.idealstate.minecraft.next.common.bytecode.api.JavaAnnotatedElement;
import team.idealstate.minecraft.next.common.bytecode.exception.BytecodeException;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;
import team.idealstate.minecraft.next.common.validate.annotation.Nullable;

public interface JavaMethod extends Java<Method>, JavaAnnotatedElement, JavaAccessible {

    @NotNull @Override
    @SuppressWarnings({"unchecked"})
    default <R extends Method> R java(@Nullable ClassLoader classLoader) throws BytecodeException {
        Class<?> declaringClass = getDeclaringClass().java(classLoader);
        try {
            JavaParameter[] parameters = getParameters();
            Class<?>[] parameterTypes =
                    Arrays.stream(parameters)
                            .map(parameter -> (Class<?>) parameter.getType().java(classLoader))
                            .toArray(Class<?>[]::new);
            return (R) declaringClass.getDeclaredMethod(getName(), parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new BytecodeException(e);
        }
    }

    @NotNull JavaClass getDeclaringClass();

    @NotNull String getName();

    @NotNull JavaParameter[] getParameters();

    @NotNull JavaClass getReturnType();

    @NotNull JavaClass[] getExceptionTypes();

    @Nullable Object getDefaultValue();

    default boolean isDefault() {
        return getDeclaringClass().isInterface() && isPublic() && !isAbstract() && !isStatic();
    }
}
