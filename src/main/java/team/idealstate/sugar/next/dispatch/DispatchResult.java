package team.idealstate.sugar.next.dispatch;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;
import team.idealstate.sugar.validate.annotation.Nullable;

@Data
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class DispatchResult<T> {

    private static final DispatchResult<?> SUCCESS_VOID = new DispatchResult<>(true, void.class, null);
    private static final DispatchResult<?> FAILURE_VOID = new DispatchResult<>(false, void.class, null);

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T> DispatchResult<T> success() {
        return (DispatchResult<T>) SUCCESS_VOID;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T> DispatchResult<T> success(@NotNull T value) {
        Validation.notNull(value, "value must not be null.");
        return success((Class<T>) value.getClass(), value);
    }

    @NotNull
    public static <T> DispatchResult<T> success(@NotNull Class<T> type, T value) {
        Validation.notNull(type, "type must not be null.");
        return new DispatchResult<>(true, type, value);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T> DispatchResult<T> failure() {
        return (DispatchResult<T>) FAILURE_VOID;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T> DispatchResult<T> failure(@NotNull T value) {
        Validation.notNull(value, "value must not be null.");
        return failure((Class<T>) value.getClass(), value);
    }

    @NotNull
    public static <T> DispatchResult<T> failure(@NotNull Class<T> type, T value) {
        Validation.notNull(type, "type must not be null.");
        return new DispatchResult<>(false, type, value);
    }

    private final boolean success;

    @NotNull
    private final Class<T> type;

    @Nullable
    private final T value;
}
