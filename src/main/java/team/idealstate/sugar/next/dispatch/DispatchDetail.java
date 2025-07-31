package team.idealstate.sugar.next.dispatch;

import team.idealstate.sugar.next.dispatch.argument.ArgumentAcceptor;
import team.idealstate.sugar.next.dispatch.argument.ArgumentCompleter;
import team.idealstate.sugar.next.dispatch.argument.ArgumentConverter;
import team.idealstate.sugar.validate.annotation.NotNull;

import java.util.List;

public interface DispatchDetail {

    /**
     * @return 当前调度的指令的参数名称列表
     */
    @NotNull
    List<String> getArguments();

    /**
     * @param argument 参数名称
     * @return 指定名称的参数所绑定的接受程序，如果不存在则应提供一个默认的程序
     */
    @NotNull
    ArgumentAcceptor getArgumentAcceptor(@NotNull String argument);

    /**
     * @param argument 参数名称
     * @param <T> 获取到的参数转换程序的目标类型将被自动指定为该类型
     * @return 指定名称的参数所绑定的转换程序，如果不存在则应提供一个默认的程序
     */
    @NotNull
    <T> ArgumentConverter<T> getArgumentConverter(@NotNull String argument);

    /**
     * @param argument 参数名称
     * @return 指定名称的参数所绑定的补全程序，如果不存在则应提供一个默认的程序
     */
    @NotNull
    ArgumentCompleter getArgumentCompleter(@NotNull String argument);

    @NotNull
    DispatchExecutor getExecutor();
}
