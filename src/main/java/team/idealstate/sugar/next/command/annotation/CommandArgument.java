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

package team.idealstate.sugar.next.command.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import team.idealstate.sugar.next.command.CommandContext;
import team.idealstate.sugar.next.command.example.ExampleCommand;
import team.idealstate.sugar.next.command.exception.CommandArgumentConversionException;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;

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
     * 此项优先级低于 {@link #converterType()}
     *
     * <p>/** 此项优先级低于 {@link #converterType()}
     *
     * @return 指定的命令参数转换器方法名称（自定义转换器方法应为公共的成员方法， 其签名结构应为 {@link ExampleCommand#convertToInt(CommandContext, String,
     *     boolean)}）， 值为 "" 时将使用 {@link #converterType()} 或是在上下文中根据参数类型获取到的转换器
     *     {@link CommandContext#getConverter(Class)}
     */
    String converter() default "";

    /**
     * 此项优先级低于 {@link #completerType()}
     *
     * @return 指定的命令参数补全器方法名称（自定义补全器方法应为公共的成员方法， 其签名结构应为 {@link ExampleCommand#completeId(CommandContext, String)}）， 值为
     *     "" 时将使用 {@link #completerType()} 或是在上下文中根据参数类型获取到的补全器 {@link CommandContext#getCompleter(Class)}
     */
    String completer() default "";

    /**
     * @return 指定的命令参数转换器类（自定义转换器应提供一个公共的无参构造方法）， 值为 {@link Converter} 时将使用 {@link #converter()} ()}
     *     或是在上下文中根据参数类型获取到的转换器 {@link CommandContext#getConverter(Class)}
     */
    Class<? extends Converter> converterType() default Converter.class;

    /**
     * @return 指定的命令参数补全器类（自定义补全器应提供一个公共的无参构造方法）， 值为 {@link Completer} 时将使用 {@link #completer()} 或是在上下文中根据参数类型获取到的补全器
     *     {@link CommandContext#getCompleter(Class)}
     */
    Class<? extends Completer> completerType() default Completer.class;

    @Data
    final class ConverterResult<T> {
        private final boolean success;
        private final T result;

        @NotNull
        public static <T> ConverterResult<T> success() {
            return success(null);
        }

        @NotNull
        public static <T> ConverterResult<T> success(T result) {
            return new ConverterResult<>(true, result);
        }

        @NotNull
        public static <T> ConverterResult<T> failure() {
            return failure(null);
        }

        @NotNull
        public static <T> ConverterResult<T> failure(T result) {
            return new ConverterResult<>(false, result);
        }
    }

    interface Converter<T> {

        @NotNull
        Class<T> getTargetType();

        /**
         * @param context 当前命令行上下文
         * @param argument 待转换的参数
         * @return 参数转换后的对象，可能为 null
         * @throws CommandArgumentConversionException 当转换失败时应抛出此异常而不是返回 null
         */
        @NotNull
        ConverterResult<T> convert(@NotNull CommandContext context, @NotNull String argument, boolean onConversion)
                throws CommandArgumentConversionException;
    }

    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    abstract class AbstractConverter<T> implements Converter<T> {
        @NonNull
        private final Class<T> targetType;

        @Override
        @NotNull
        public final Class<T> getTargetType() {
            return targetType;
        }

        @NotNull
        protected abstract ConverterResult<T> doConvert(@NotNull CommandContext context, @NotNull String argument)
                throws CommandArgumentConversionException;

        protected abstract boolean canBeConvert(@NotNull CommandContext context, @NotNull String argument);

        @NotNull
        @Override
        public final ConverterResult<T> convert(
                @NotNull CommandContext context, @NotNull String argument, boolean onConversion)
                throws CommandArgumentConversionException {
            Validation.notNull(context, "context must not be null.");
            Validation.notNull(argument, "argument must not be null.");
            boolean canBeConvert = canBeConvert(context, argument);
            if (!onConversion) {
                return canBeConvert ? ConverterResult.success() : ConverterResult.failure();
            } else if (!canBeConvert) {
                throw new CommandArgumentConversionException(String.format(
                        "The argument '%s' cannot be convert to the targetType '%s'.", argument, getTargetType()));
            }
            ConverterResult<T> result = doConvert(context, argument);
            if (!result.isSuccess()) {
                return result;
            }
            T done = result.getResult();
            if (done == null) {
                return result;
            }
            Class<?> targetType = Validation.requireNotNull(getTargetType(), "targetType must not be null.");
            if (targetType.isInstance(done)) {
                return result;
            }
            throw new CommandArgumentConversionException(
                    "The type of the argument is not matched with the targetType.");
        }
    }

    interface Completer {

        @NotNull
        List<String> complete(@NotNull CommandContext context, @NotNull String argument);
    }
}
