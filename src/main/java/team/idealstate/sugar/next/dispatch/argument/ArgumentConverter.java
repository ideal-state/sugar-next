package team.idealstate.sugar.next.dispatch.argument;

import team.idealstate.sugar.next.dispatch.DispatchContext;
import team.idealstate.sugar.validate.annotation.NotNull;
import team.idealstate.sugar.validate.annotation.Nullable;

/**
 * <p>参数转换程序</p>
 * 用于将 {@link String} 类型的输入参数转换为调度处理器中对应形参的实际类型对象
 *
 * @param <T> 转换后的目标类型
 */
@FunctionalInterface
public interface ArgumentConverter<T> {

    /**
     * 执行参数转换的主要接口
     *
     * @param context 当前调度指令持有的上下文
     * @param index 当前参数在指令参数列表中的实际索引
     * @param input 当前参数的实际输入值
     * @return 转换后的参数对象，其值允许为 null
     */
    @Nullable
    T convert(@NotNull DispatchContext context, int index, @NotNull String input);
}
