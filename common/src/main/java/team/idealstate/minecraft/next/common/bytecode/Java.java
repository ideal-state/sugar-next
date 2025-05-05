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

package team.idealstate.minecraft.next.common.bytecode;

import team.idealstate.minecraft.next.common.bytecode.api.member.JavaClass;
import team.idealstate.minecraft.next.common.bytecode.exception.BytecodeException;
import team.idealstate.minecraft.next.common.bytecode.exception.BytecodeParsingException;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;
import team.idealstate.minecraft.next.common.validate.annotation.Nullable;

public interface Java<T> {

    @NotNull static JavaClass typeof(@NotNull Object object) throws BytecodeParsingException {
        return typeof(object.getClass());
    }

    @NotNull static JavaClass typeof(@NotNull Class<?> cls) throws BytecodeParsingException {
        return typeof(cls.getName());
    }

    @NotNull static JavaClass typeof(@NotNull String className) throws BytecodeParsingException {
        return typeof(className, new JavaCache());
    }

    @NotNull static JavaClass typeof(@NotNull Object object, @NotNull JavaCache cache)
            throws BytecodeParsingException {
        return typeof(object.getClass(), cache);
    }

    @NotNull static JavaClass typeof(@NotNull Class<?> cls, @NotNull JavaCache cache)
            throws BytecodeParsingException {
        return typeof(cls.getName(), cache);
    }

    @NotNull static JavaClass typeof(@NotNull String className, @NotNull JavaCache cache)
            throws BytecodeParsingException {
        return typeof(className, cache, null);
    }

    @NotNull static JavaClass typeof(
            @NotNull Object object, @NotNull JavaCache cache, ClassLoader classLoader)
            throws BytecodeParsingException {
        Validation.notNull(object, "object must not be null.");
        Validation.notNull(cache, "cache must not be null.");
        return typeof(object.getClass(), cache, classLoader);
    }

    @NotNull static JavaClass typeof(
            @NotNull Class<?> cls, @NotNull JavaCache cache, ClassLoader classLoader)
            throws BytecodeParsingException {
        Validation.notNull(cls, "class must not be null.");
        Validation.notNull(cache, "cache must not be null.");
        return typeof(cls.getName(), cache, classLoader);
    }

    @NotNull static JavaClass typeof(
            @NotNull String className, @NotNull JavaCache cache, ClassLoader classLoader)
            throws BytecodeParsingException {
        Validation.notNull(className, "className must not be null.");
        Validation.notNull(cache, "cache must not be null.");
        return cache.forName(className, classLoader);
    }

    @NotNull default <R extends T> R java() throws BytecodeException {
        return java(getClass().getClassLoader());
    }

    @NotNull <R extends T> R java(@Nullable ClassLoader classLoader) throws BytecodeException;
}
