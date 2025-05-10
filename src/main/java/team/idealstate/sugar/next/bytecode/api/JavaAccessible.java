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

package team.idealstate.sugar.next.bytecode.api;

import team.idealstate.sugar.internal.org.objectweb.asm.Opcodes;

public interface JavaAccessible {

    int getAccess();

    default boolean isPublic() {
        int access = getAccess();
        return (access & Opcodes.ACC_PUBLIC) != 0;
    }

    default boolean isProtected() {
        int access = getAccess();
        return (access & Opcodes.ACC_PROTECTED) != 0;
    }

    default boolean isInternal() {
        return !isPublic() && !isProtected() && !isPrivate();
    }

    default boolean isPrivate() {
        int access = getAccess();
        return (access & Opcodes.ACC_PRIVATE) != 0;
    }

    default boolean isStatic() {
        int access = getAccess();
        return (access & Opcodes.ACC_STATIC) != 0;
    }

    default boolean isFinal() {
        int access = getAccess();
        return (access & Opcodes.ACC_FINAL) != 0;
    }

    default boolean isAbstract() {
        int access = getAccess();
        return (access & Opcodes.ACC_ABSTRACT) != 0;
    }

    default boolean isNative() {
        int access = getAccess();
        return (access & Opcodes.ACC_NATIVE) != 0;
    }

    default boolean isSynchronized() {
        int access = getAccess();
        return (access & Opcodes.ACC_SYNCHRONIZED) != 0;
    }

    default boolean isTransient() {
        int access = getAccess();
        return (access & Opcodes.ACC_TRANSIENT) != 0;
    }

    default boolean isVolatile() {
        int access = getAccess();
        return (access & Opcodes.ACC_VOLATILE) != 0;
    }

    default boolean isStrict() {
        int access = getAccess();
        return (access & Opcodes.ACC_STRICT) != 0;
    }

    default boolean isInterface() {
        int access = getAccess();
        return (access & Opcodes.ACC_INTERFACE) != 0;
    }

    default boolean isBridge() {
        int access = getAccess();
        return (access & Opcodes.ACC_BRIDGE) != 0;
    }

    default boolean isVarArgs() {
        int access = getAccess();
        return (access & Opcodes.ACC_VARARGS) != 0;
    }

    default boolean isSynthetic() {
        int access = getAccess();
        return (access & Opcodes.ACC_SYNTHETIC) != 0;
    }

    default boolean isAnnotation() {
        int access = getAccess();
        return (access & Opcodes.ACC_ANNOTATION) != 0;
    }

    default boolean isEnum() {
        int access = getAccess();
        return (access & Opcodes.ACC_ENUM) != 0;
    }

    default boolean isMandated() {
        int access = getAccess();
        return (access & Opcodes.ACC_MANDATED) != 0;
    }
}
