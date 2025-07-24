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

package team.idealstate.sugar.next.lang;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import team.idealstate.sugar.next.function.Lazy;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;

public abstract class WrappedType<T> {

    private static final Map<Class<?>, Class<?>> PRIMITIVE_TYPES;
    private static final Map<Class<?>, Lazy<WrappedType<?>>> WRAPPED_TYPES;

    static {
        Map<Class<?>, Class<?>> primitiveTypes = new HashMap<>();
        Map<Class<?>, Lazy<WrappedType<?>>> wrappedTypes = new HashMap<>();
        wrappedTypes.put(byte.class, Lazy.of(() -> new AutoWrapWrappedType<>(byte.class, Byte.class)));
        primitiveTypes.put(Byte.class, byte.class);
        wrappedTypes.put(short.class, Lazy.of(() -> new AutoWrapWrappedType<>(short.class, Short.class)));
        primitiveTypes.put(Short.class, short.class);
        wrappedTypes.put(int.class, Lazy.of(() -> new AutoWrapWrappedType<>(int.class, Integer.class)));
        primitiveTypes.put(Integer.class, int.class);
        wrappedTypes.put(long.class, Lazy.of(() -> new AutoWrapWrappedType<>(long.class, Long.class)));
        primitiveTypes.put(Long.class, long.class);
        wrappedTypes.put(float.class, Lazy.of(() -> new AutoWrapWrappedType<>(float.class, Float.class)));
        primitiveTypes.put(Float.class, float.class);
        wrappedTypes.put(double.class, Lazy.of(() -> new AutoWrapWrappedType<>(double.class, Double.class)));
        primitiveTypes.put(Double.class, double.class);
        wrappedTypes.put(boolean.class, Lazy.of(() -> new AutoWrapWrappedType<>(boolean.class, Boolean.class)));
        primitiveTypes.put(Boolean.class, boolean.class);
        wrappedTypes.put(char.class, Lazy.of(() -> new AutoWrapWrappedType<>(char.class, Character.class)));
        primitiveTypes.put(Character.class, char.class);
        wrappedTypes.put(void.class, Lazy.of(VoidWrappedType::new));
        primitiveTypes.put(Void.class, void.class);
        WRAPPED_TYPES = Collections.unmodifiableMap(wrappedTypes);
        PRIMITIVE_TYPES = Collections.unmodifiableMap(primitiveTypes);
    }

    private final Class<?> primitiveType;
    private final Class<T> wrappedType;

    private WrappedType(@NotNull Class<?> primitiveType, @NotNull Class<T> wrappedType) {
        Validation.notNull(primitiveType, "Primitive type must not be null.");
        Validation.notNull(wrappedType, "Wrapped type must not be null.");
        this.primitiveType = primitiveType;
        this.wrappedType = wrappedType;
    }

    @NotNull
    public static <T> WrappedType<T> of(@NotNull Class<?> primitiveType) {
        return ofPrimitiveType(primitiveType);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T> WrappedType<T> ofPrimitiveType(@NotNull Class<?> primitiveType) {
        Lazy<WrappedType<?>> lazy = WRAPPED_TYPES.get(primitiveType);
        if (lazy == null) {
            throw new IllegalArgumentException(
                    String.format("Primitive type '%s' cannot not be mapping a WrappedType.", primitiveType.getName()));
        }
        return Validation.requireNotNull((WrappedType<T>) lazy.get(), "WrappedType must not be null.");
    }

    @NotNull
    public static <T> WrappedType<T> ofWrappedType(@NotNull Class<T> wrappedType) {
        Validation.notNull(wrappedType, "Wrapped type must not be null.");
        Class<?> primitiveType = PRIMITIVE_TYPES.get(wrappedType);
        if (primitiveType == null) {
            throw new IllegalArgumentException(
                    String.format("Wrapped type '%s' cannot not be mapping a primitive type.", wrappedType.getName()));
        }
        return ofPrimitiveType(primitiveType);
    }

    @NotNull
    public final Class<?> getType() {
        return getWrappedType();
    }

    @NotNull
    public final Class<?> getPrimitiveType() {
        return primitiveType;
    }

    @NotNull
    public final Class<T> getWrappedType() {
        return wrappedType;
    }

    @NotNull
    protected abstract <R> T doWrap(@NotNull R primitiveValue);

    @NotNull
    public final <R> T wrap(@NotNull R primitiveValue) {
        Validation.notNull(primitiveValue, "Primitive value must not be null.");
        return Validation.requireNotNull(doWrap(primitiveValue), "Wrapped value must not be null.");
    }

    @NotNull
    protected abstract <R> R doUnwrap(@NotNull T wrappedValue);

    @NotNull
    public final <R> R unwrap(@NotNull T wrappedValue) {
        Validation.notNull(wrappedValue, "Wrapped value must not be null.");
        return Validation.requireNotNull(doUnwrap(wrappedValue), "Primitive value must not be null.");
    }

    private static final class AutoWrapWrappedType<T> extends WrappedType<T> {

        private AutoWrapWrappedType(@NotNull Class<?> primitiveType, @NotNull Class<T> wrappedType) {
            super(primitiveType, wrappedType);
        }

        @SuppressWarnings({"UnnecessaryLocalVariable", "unchecked"})
        @NotNull
        @Override
        protected <R> T doWrap(@NotNull R primitiveValue) {
            Object instance = primitiveValue;
            Class<T> wrappedType = getWrappedType();
            Validation.is(
                    wrappedType.isInstance(instance),
                    String.format(
                            "Primitive value '%s' must be an instance of wrappedType '%s'.",
                            instance, wrappedType.getName()));
            return (T) instance;
        }

        @SuppressWarnings({"UnnecessaryLocalVariable", "unchecked"})
        @NotNull
        @Override
        protected <R> R doUnwrap(@NotNull T wrappedValue) {
            Object instance = wrappedValue;
            return (R) instance;
        }
    }

    private static final class VoidWrappedType extends WrappedType<Void> {

        private VoidWrappedType() {
            super(void.class, Void.class);
        }

        @NotNull
        @Override
        protected <R> Void doWrap(@NotNull R primitiveValue) {
            throw new UnsupportedOperationException("Cannot wrap primitive value of type 'void'.");
        }

        @NotNull
        @Override
        protected <R> R doUnwrap(@NotNull Void wrappedValue) {
            throw new UnsupportedOperationException("Cannot unwrap wrapped value of type 'Void'.");
        }
    }
}
