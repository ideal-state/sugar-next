/*
 *    Copyright 2025 ideal-state
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package team.idealstate.minecraft.next.common.command.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import team.idealstate.minecraft.next.common.command.CommandContext;
import team.idealstate.minecraft.next.common.command.example.ExampleCommand;
import team.idealstate.minecraft.next.common.command.exception.CommandArgumentConversionException;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;
import team.idealstate.minecraft.next.common.validate.annotation.Nullable;

/**
 * 用于标注命令行参数
 *
 * @see ExampleCommand
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CommandArgument {

    /**
     * @return 指定的命令参数名称，值为 "" 时将使用方法参数名称 （须要注意的是，方法的参数名称仅在编译器启用了特定参数 <code>-parameters</code> 时保留）
     * @see CommandHandler#value() 参数命名规范（规则）遵循此处
     */
    String value() default "";

    /**
     * @return 指定的命令参数转换器类（自定义转换器应提供一个公共的无参构造方法）， 值为 {@link Converter} 时将使用全局转换器（可能为 null）
     */
    Class<? extends Converter> converter() default Converter.class;

    /**
     * @return 指定的命令参数补全器类（自定义补全器应提供一个公共的无参构造方法）， 值为 {@link Completer} 时将使用 {@link
     *     #completerMethod()} 或全局补全器（可能为 null）
     */
    Class<? extends Completer> completer() default Completer.class;

    /**
     * 此项优先级低于 {@link #completer()}
     *
     * @return 指定的命令参数补全器方法名称（自定义补全器方法应为公共的成员方法， 其签名结构应为 {@link
     *     ExampleCommand#getConfigs(CommandContext, String)}）， 值为 "" 时将使用 {@link #completer()}
     *     或全局补全器（可能为 null）
     */
    String completerMethod() default "";

    interface Converter<T> {

        @NotNull Class<T> getTargetType();

        /**
         * @param context 当前命令行上下文
         * @param argument 待转换的参数
         * @return 参数转换后的对象，可能为 null
         * @throws CommandArgumentConversionException 当转换失败时应抛出此异常而不是返回 null
         */
        @Nullable T doConvert(@NotNull CommandContext context, @NotNull String argument)
                throws CommandArgumentConversionException;

        /**
         * @see Converter#doConvert(CommandContext, String)
         */
        @Nullable default T convert(@NotNull CommandContext context, @NotNull String argument)
                throws CommandArgumentConversionException {
            T done = doConvert(context, argument);
            if (done == null) {
                return null;
            }
            Class<?> targetType =
                    Validation.requireNotNull(getTargetType(), "targetType must not be null.");
            if (targetType.isInstance(done)) {
                return done;
            }
            throw new CommandArgumentConversionException(
                    "The type of the argument is not matched with the target type.");
        }

        /**
         * @param context 当前命令行上下文
         * @param argument 待转换的参数
         * @return 参数是否可以被转换
         */
        boolean canCovert(@NotNull CommandContext context, @NotNull String argument);
    }

    interface Completer {

        @NotNull List<String> complete(@NotNull CommandContext context, @NotNull String argument);
    }
}
