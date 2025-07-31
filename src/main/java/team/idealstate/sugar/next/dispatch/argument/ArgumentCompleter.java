package team.idealstate.sugar.next.dispatch.argument;

import team.idealstate.sugar.next.dispatch.DispatchContext;
import team.idealstate.sugar.validate.annotation.NotNull;

import java.util.List;

/**
 * <p>参数补全列表提供程序</p>
 *
 * 为可能不完整的输入参数提供可选的补全列表
 */
public interface ArgumentCompleter {

    /**
     * 提供参数补全列表的主要接口
     *
     * @param context 当前调度指令持有的上下文
     * @param index 当前参数在指令参数列表中的实际索引
     * @param input 当前参数的实际输入值
     * @return 补全列表，其值不允许为 null，但允许为空列表
     */
    @NotNull
    List<String> complete(@NotNull DispatchContext context, int index, @NotNull String input);
}
