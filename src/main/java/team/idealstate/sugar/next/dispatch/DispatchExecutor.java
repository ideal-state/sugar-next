package team.idealstate.sugar.next.dispatch;

import team.idealstate.sugar.validate.annotation.NotNull;

@FunctionalInterface
public interface DispatchExecutor {

    @NotNull
    <T> DispatchResult<T> execute(@NotNull DispatchContext context, @NotNull String... arguments);
}
