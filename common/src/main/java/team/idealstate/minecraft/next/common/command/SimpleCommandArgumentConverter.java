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

package team.idealstate.minecraft.next.common.command;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import team.idealstate.minecraft.next.common.command.annotation.CommandArgument;
import team.idealstate.minecraft.next.common.command.annotation.CommandArgument.ConverterResult;
import team.idealstate.minecraft.next.common.command.exception.CommandArgumentConversionException;
import team.idealstate.minecraft.next.common.command.exception.CommandException;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

final class SimpleCommandArgumentConverter<T> extends CommandArgument.AbstractConverter<T> {

    private final Object command;
    private final Method method;

    SimpleCommandArgumentConverter(
            @NotNull Class<T> targetType, @NotNull Object command, @NotNull Method method) {
        super(targetType);
        Validation.notNull(command, "command cannot be null.");
        Validation.notNull(method, "method cannot be null.");
        this.command = command;
        this.method = method;
    }

    @Override
    @SuppressWarnings({"unchecked"})
    protected @NotNull ConverterResult<T> doConvert(
            @NotNull CommandContext context, @NotNull String argument)
            throws CommandArgumentConversionException {
        try {
            ConverterResult<T> result =
                    (ConverterResult<T>) method.invoke(command, context, argument, true);
            return Validation.requireNotNull(result, "converter result cannot be null.");
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new CommandException(e);
        }
    }

    @Override
    @SuppressWarnings({"unchecked"})
    protected boolean canBeConvert(@NotNull CommandContext context, @NotNull String argument) {
        try {
            ConverterResult<T> result =
                    (ConverterResult<T>) method.invoke(command, context, argument, false);
            return Validation.requireNotNull(result, "converter result cannot be null.")
                    .isSuccess();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new CommandException(e);
        }
    }
}
