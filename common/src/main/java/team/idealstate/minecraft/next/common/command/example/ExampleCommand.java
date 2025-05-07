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

package team.idealstate.minecraft.next.common.command.example;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import team.idealstate.minecraft.next.common.command.CommandContext;
import team.idealstate.minecraft.next.common.command.CommandResult;
import team.idealstate.minecraft.next.common.command.annotation.CommandArgument;
import team.idealstate.minecraft.next.common.command.annotation.CommandArgument.ConverterResult;
import team.idealstate.minecraft.next.common.command.annotation.CommandHandler;
import team.idealstate.minecraft.next.common.command.exception.CommandArgumentConversionException;
import team.idealstate.minecraft.next.common.string.StringUtils;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

public final class ExampleCommand {

    private final List<String> ids = Arrays.asList("aaa", "bbb");

    @CommandHandler
    public CommandResult reload() {
        return CommandResult.success("reload");
    }

    @CommandHandler("reload {id}")
    public CommandResult reload(@CommandArgument(completer = "completeId") String id) {
        return CommandResult.success(String.format("reload: %s", id));
    }

    @NotNull public List<String> completeId(@NotNull CommandContext context, @NotNull String argument) {
        if (argument.isEmpty()) {
            return ids;
        }
        return ids.stream()
                .filter(s -> s.toLowerCase().startsWith(argument.toLowerCase()))
                .collect(Collectors.toList());
    }

    @CommandHandler("sum {a} {b}")
    public CommandResult sum(
            CommandContext context,
            @CommandArgument(value = "a", converter = "convertToInt") Integer first,
            @CommandArgument(value = "b", converter = "convertToInt") Integer second
    ) {
        return CommandResult.success(String.format("context: %s, sum: %s", context, first + second));
    }

    @CommandHandler("sum {a} {b} {c}")
    public CommandResult sum(
            CommandContext context,
            @CommandArgument(value = "a", converter = "convertToInt") Integer first,
            @CommandArgument(value = "b", converter = "convertToInt") Integer second,
            @CommandArgument(value = "c", converter = "convertToInt") Integer third
    ) {
        return CommandResult.success(String.format("context: %s, sum: %s", context, first + second + third));
    }

    @NotNull
    public ConverterResult<Integer> convertToInt(@NotNull CommandContext context, @NotNull String argument, boolean onConversion) throws CommandArgumentConversionException {
        boolean canBeConvert = StringUtils.isInteger(argument);
        if (!onConversion) {
            return canBeConvert ? ConverterResult.success() : ConverterResult.failure();
        }
        if (!canBeConvert) {
            throw new CommandArgumentConversionException(String.format("The argument '%s' cannot be convert to int.", argument));
        }
        return ConverterResult.success(Integer.parseInt(argument));
    }

}
