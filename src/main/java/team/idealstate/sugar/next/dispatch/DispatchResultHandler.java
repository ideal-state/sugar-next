package team.idealstate.sugar.next.dispatch;

import team.idealstate.sugar.validate.annotation.NotNull;

@FunctionalInterface
public interface DispatchResultHandler<T> {

    void handle(@NotNull DispatchContext context, @NotNull DispatchResult<T> result);
}
