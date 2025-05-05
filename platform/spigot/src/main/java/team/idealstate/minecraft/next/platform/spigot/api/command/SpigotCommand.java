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

package team.idealstate.minecraft.next.platform.spigot.api.command;

import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import team.idealstate.minecraft.next.common.command.CommandContext;
import team.idealstate.minecraft.next.common.command.CommandLine;
import team.idealstate.minecraft.next.common.command.CommandResult;
import team.idealstate.minecraft.next.common.validation.annotation.NotNull;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class SpigotCommand implements TabExecutor {

    @NotNull public static SpigotCommand of(@NotNull String name, @NotNull Object command) {
        return new SpigotCommand(CommandLine.of(name, command), null);
    }

    @NotNull public static SpigotCommand of(
            @NotNull String name, @NotNull Object command, String failureMessage) {
        Objects.requireNonNull(name, "name must not be null.");
        Objects.requireNonNull(command, "command must not be null.");
        CommandLine commandLine = CommandLine.of(name, command);
        return new SpigotCommand(commandLine, failureMessage);
    }

    @NonNull private final CommandLine commandLine;
    private final String failureMessage;

    @Override
    public boolean onCommand(
            CommandSender commandSender, Command command, String label, String[] arguments) {
        SpigotCommandSender sender = SpigotCommandSender.of(commandSender);
        CommandContext context = CommandContext.of(sender);
        CommandResult result = commandLine.execute(context, arguments);
        String message = result.getMessage();
        boolean success = result.isSuccess();
        if (message != null) {
            commandSender.sendMessage(message);
        } else if (!success && failureMessage != null) {
            commandSender.sendMessage(failureMessage);
        }
        return success;
    }

    @Override
    public List<String> onTabComplete(
            CommandSender commandSender, Command command, String label, String[] arguments) {
        SpigotCommandSender sender = SpigotCommandSender.of(commandSender);
        CommandContext context = CommandContext.of(sender);
        return commandLine.complete(context, arguments);
    }
}
