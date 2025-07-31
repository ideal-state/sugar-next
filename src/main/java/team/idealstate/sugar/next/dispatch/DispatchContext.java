package team.idealstate.sugar.next.dispatch;

import team.idealstate.sugar.validate.annotation.NotNull;
import team.idealstate.sugar.validate.annotation.Nullable;

import java.util.List;
import java.util.Set;

public interface DispatchContext {

    /**
     * @param <T> 见 {@link DispatchRequester#getHolder()}
     * @return 当前调度的指令的请求者
     */
    @NotNull
    <T> DispatchRequester<T> getRequester();

    /**
     * @return 当前调度的指令绑定的指令调度处理程序的细节
     */
    @NotNull
    DispatchDetail getDetail();

    /**
     * @return 当前调度的指令的实际输入参数列表
     */
    @NotNull
    List<String> getInputs();

    /**
     * @param argument 参数名称
     * @param <T> 获取到的参数将被自动强制转换为该类型
     * @return 指定名称的参数值，其值可能为 null，当参数为字面量参数时，其值与参数名称相同
     */
    @Nullable
    <T> T getArgument(@NotNull String argument);

    /**
     * 将指定名称的参数值设置为指定值
     *
     * @param argument 参数名称
     * @param value 参数值
     */
    void setArgument(@NotNull String argument, Object value);

    /**
     * @return 当前调度的指令的附件（键）列表
     */
    @NotNull
    Set<String> getAttachments();

    /**
     * @param key 附件键
     * @param <T> 获取到的附件将被自动强制转换为该类型
     * @return 指定键的附件值，其值可能为 null
     */
    @Nullable
    <T> T getAttachment(@NotNull String key);

    /**
     * 将指定键的附件值设置为指定值
     *
     * @param key 附件键
     * @param value 附件值
     */
    void setAttachment(@NotNull String key, Object value);
}
