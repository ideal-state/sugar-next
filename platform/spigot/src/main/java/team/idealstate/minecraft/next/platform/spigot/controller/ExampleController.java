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

package team.idealstate.minecraft.next.platform.spigot.controller;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import team.idealstate.minecraft.next.common.banner.Banner;
import team.idealstate.minecraft.next.common.command.Command;
import team.idealstate.minecraft.next.common.command.CommandContext;
import team.idealstate.minecraft.next.common.command.CommandResult;
import team.idealstate.minecraft.next.common.command.annotation.CommandArgument;
import team.idealstate.minecraft.next.common.command.annotation.CommandArgument.ConverterResult;
import team.idealstate.minecraft.next.common.command.annotation.CommandHandler;
import team.idealstate.minecraft.next.common.command.exception.CommandArgumentConversionException;
import team.idealstate.minecraft.next.common.context.ContextHolder;
import team.idealstate.minecraft.next.common.context.annotation.component.Controller;
import team.idealstate.minecraft.next.common.context.annotation.feature.Environment;
import team.idealstate.minecraft.next.common.context.aware.ContextHolderAware;
import team.idealstate.minecraft.next.common.logging.Log;
import team.idealstate.minecraft.next.common.string.StringUtils;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;
import team.idealstate.minecraft.next.platform.spigot.api.placeholder.Placeholder;

@Controller("minecraftnext")
@Environment("DEBUG")
public final class ExampleController implements ContextHolderAware, Command, Placeholder {

    @CommandHandler
    public CommandResult show() {
        Log.info("show()");
        return CommandResult.success();
    }

    @CommandHandler("show {message}")
    public CommandResult show(@CommandArgument(completer = "completeMessage") String message) {
        Log.info(String.format("show(String): %s", message));
        return CommandResult.success(message);
    }

    @CommandHandler("show {target} {message}")
    public CommandResult show(
            @CommandArgument(value = "target", completer = "completePlayer", converter = "convertToPlayer") Player player,
            @CommandArgument(completer = "completeMessage") String message
    ) {
        Log.info(String.format("show(Player, String): %s, %s", player, message));
        player.sendMessage(message);
        return CommandResult.success();
    }

    @NotNull
    public List<String> completePlayer(@NotNull CommandContext context, @NotNull String argument) {
        String lowerCase = argument.toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .map(String::toLowerCase)
                .filter(s -> s.startsWith(lowerCase))
                .collect(Collectors.toList());
    }

    @NotNull
    public List<String> completeMessage(@NotNull CommandContext context, @NotNull String argument) {
        return Arrays.asList("minecraft-next", "hello-next");
    }

    @NotNull
    public ConverterResult<Player> convertToPlayer(@NotNull CommandContext context, @NotNull String argument, boolean onConversion) throws CommandArgumentConversionException {
        Player player = Bukkit.getPlayer(argument);
        boolean canBeConvert = player != null && player.isOnline();
        if (!onConversion) {
            return canBeConvert ? ConverterResult.success() : ConverterResult.failure();
        }
        if (!canBeConvert) {
            throw new CommandArgumentConversionException(String.format("The argument '%s' cannot be convert to player.", argument));
        }
        return ConverterResult.success(player);
    }

    @CommandHandler
    public CommandResult banner(CommandContext context) {
        Log.info(String.format("banner(CommandContext) %s", context.getSender().getUniqueId()));
        ContextHolder holder = contextHolder;
        if (holder == null) {
            return CommandResult.failure("ContextHolder is null");
        } else {
            Banner.lines(holder.getClass()).forEach(Log::info);
        }
        return CommandResult.success();
    }

    private volatile ContextHolder contextHolder;

    @Override
    public void setContextHolder(@NotNull ContextHolder contextHolder) {
        this.contextHolder = contextHolder;
    }
}
