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
import team.idealstate.minecraft.next.common.command.annotation.CommandHandler;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

public final class ExampleCommand {

    private final List<String> configs = Arrays.asList("aaa", "bbb");

    @CommandHandler
    public CommandResult reload() {
        return CommandResult.success("重载成功");
    }

    @CommandHandler("reload {id}")
    public CommandResult reload(@CommandArgument(completerMethod = "getConfigs") String id) {
        return CommandResult.success("重载成功: " + id);
    }

    @NotNull public List<String> getConfigs(@NotNull CommandContext context, @NotNull String argument) {
        if (argument.isEmpty()) {
            return configs;
        }
        return configs.stream()
                .filter(s -> s.toLowerCase().startsWith(argument.toLowerCase()))
                .collect(Collectors.toList());
    }

    @CommandHandler("fuck {name}")
    public CommandResult fuck(CommandContext context, @CommandArgument("name") String target) {
        return CommandResult.success("fuck: " + target + " " + context);
    }
}
