package team.idealstate.sugar.next.dispatch.argument;

import team.idealstate.sugar.next.dispatch.DispatchContext;
import team.idealstate.sugar.validate.annotation.NotNull;

/**
 * <p>参数接受判断程序</p>
 *
 * 用于判断实际输入参数是否被调度程序接受
 */
@FunctionalInterface
public interface ArgumentAcceptor {

    /**
     * 执行参数接受判断的主要接口
     *
     * @param context 当前调度指令持有的上下文
     * @param index 当前参数在指令参数列表中的实际索引
     * @param input 当前参数的实际输入值
     * @return 是否接受该参数
     */
    boolean accept(@NotNull DispatchContext context, int index, @NotNull String input);
}
