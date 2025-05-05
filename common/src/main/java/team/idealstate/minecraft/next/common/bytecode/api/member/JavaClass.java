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

import team.idealstate.minecraft.next.common.bytecode.Java;
import team.idealstate.minecraft.next.common.bytecode.api.JavaAccessible;
import team.idealstate.minecraft.next.common.bytecode.api.JavaAnnotatedElement;
import team.idealstate.minecraft.next.common.bytecode.api.JavaType;
import team.idealstate.minecraft.next.common.bytecode.exception.BytecodeException;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;
import team.idealstate.minecraft.next.common.validate.annotation.Nullable;

public interface JavaClass extends Java<Class<?>>, JavaType, JavaAccessible, JavaAnnotatedElement {

    @NotNull @Override
    @SuppressWarnings({"unchecked"})
    default <R extends Class<?>> R java(@Nullable ClassLoader classLoader)
            throws BytecodeException {
        try {
            return (R) Class.forName(getName(), false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new BytecodeException(e);
        }
    }

    @NotNull JavaPackage getPackage();

    @Nullable JavaClass getSuperClass();

    @NotNull JavaClass[] getInterfaces();

    @Nullable JavaClass getOuterClass();

    @NotNull JavaClass[] getInnerClasses();

    @NotNull JavaConstructor[] getConstructors();

    @NotNull JavaField[] getFields();

    @NotNull JavaMethod[] getMethods();
}
