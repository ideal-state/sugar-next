package team.idealstate.sugar.next.dispatch.argument.annotation;

import team.idealstate.sugar.next.dispatch.DispatchContext;
import team.idealstate.sugar.next.dispatch.DispatchDetail;
import team.idealstate.sugar.next.dispatch.Dispatcher;
import team.idealstate.sugar.next.dispatch.argument.ArgumentAcceptor;
import team.idealstate.sugar.next.dispatch.argument.ArgumentCompleter;
import team.idealstate.sugar.next.dispatch.argument.ArgumentConverter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 为指令参数调度程序方法中的形参配置其相关元数据和组件。
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Argument {

    /**
     * @return 用于指定形参所对应的指令占位符名称。
     * 当使用默认值时，通常表示该形参名称即为占位符名称。
     * 注意：当编译类文件时未明确指定编译参数 `--parameter` 时，字节码中的实际形参名称可能与源代码中的形参名称不同。
     */
    String value() default "";

    /**
     * @return 用于指定形参所对应的参数接受程序。
     * 当使用默认值时，通常表示无条件接受该参数。
     */
    Class<? extends ArgumentAcceptor> acceptor() default ArgumentAcceptor.class;

    /**
     * @return 用于指定形参所对应的参数转换程序。
     * 当使用默认值时，通常表示该参数无须转换或于此参数关联的 {@link DispatchDetail} 中已存在对应形参类型的参数转换程序。
     */
    Class<? extends ArgumentConverter> converter() default ArgumentConverter.class;

    /**
     * @return 用于指定形参所对应的参数补全程序。
     * 当使用默认值时，通常表示该参数将不提供补全内容。
     */
    Class<? extends ArgumentCompleter> completer() default ArgumentCompleter.class;
}
