package team.idealstate.sugar.next.dispatch.annotation;

import team.idealstate.sugar.next.dispatch.DispatchContext;
import team.idealstate.sugar.next.dispatch.DispatchRequester;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于指定一个公开方法作为指令参数调度程序。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Dispatch {

    /**
     * <code>
     *     &#064;Dispatch<br>
     *     public void reload() {<br>
     *         // ...<br>
     *     }<br>
     * <br>
     *     &#064;Dispatch(value = "say {message}", open = true)<br>
     *     public void say(String message) {<br>
     *         // ...<br>
     *     }<br>
     * <br>
     *     &#064;Dispatch("bc {message}")<br>
     *     public void broadcast(String message) {<br>
     *         // ...<br>
     *     }<br>
     * </code>
     * 方法形参的顺序无须与指令参数模版中的参数顺序一致，
     * 当方法形参类型为 {@link DispatchContext} 时，将自动注入当前调度指令的上下文。
     *
     * @return 用于指定调度的指令参数模版，当不指定此值时，将使用当前方法名，
     * 非占位符部分则为字面量参数，
     * 其中 <code>{...}</code> 将作为占位符（要求存在同名的方法形参）用于表示运行时接收的实际对象。
     */
    String value() default "";
    
    /**
     * @return 该指令参数调度程序是否为开放类型，
     * 在未明确指定开放类型时，指令将只接受来自管理员（{@link DispatchRequester#isAdmin()}）的调度请求。
     */
    boolean open() default false;
}
