package team.idealstate.sugar.next.dispatch;

import team.idealstate.sugar.next.dispatch.argument.ArgumentConverter;
import team.idealstate.sugar.validate.annotation.NotNull;
import team.idealstate.sugar.validate.annotation.Nullable;

import java.util.List;

public interface Dispatcher {

    @Nullable
    <T> ArgumentConverter<T> setArgumentConverter(@NotNull Class<T> type, ArgumentConverter<T> argumentConverter);

    @Nullable
    <T> DispatchResultHandler<T> setResultHandler(@NotNull Class<T> type, DispatchResultHandler<T> resultHandler);

    void install(@NotNull String name, @NotNull Object details);

    void install(@NotNull String name, @NotNull List<DispatchDetail> details);

    @NotNull
    List<DispatchDetail> uninstall(@NotNull String name);

    @NotNull
    <T> DispatchResult<T> dispatch(@NotNull DispatchRequester<?> requester, @NotNull String... inputs);

    @NotNull
    List<String> complete(@NotNull DispatchRequester<?> requester, @NotNull String... inputs);
}
