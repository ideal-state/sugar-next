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

import java.util.Collections;
import java.util.Map;

import team.idealstate.minecraft.next.common.command.annotation.CommandArgument;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;
import team.idealstate.minecraft.next.common.validate.annotation.Nullable;

public interface CommandContext extends Map<String, Object> {

    @NotNull static CommandContext of(@NotNull CommandSender sender) {
        return of(sender, Collections.emptyMap());
    }

    @NotNull static CommandContext of(@NotNull CommandSender sender, @NotNull Map<String, Object> map) {
        Validation.notNull(sender, "sender must not be null.");
        Validation.notNull(map, "map must not be null.");
        return new SimpleCommandContext(sender, map);
    }

    @NotNull CommandSender getSender();

    @Nullable
    CommandArgument.Completer getCompleter(@NotNull Class<?> argumentType);

    void setCompleter(@NotNull Class<?> argumentType, @Nullable CommandArgument.Completer completer);

    @Nullable
    <T> CommandArgument.Converter<T> getConverter(@NotNull Class<T> argumentType);

    <T> void setConverter(@NotNull Class<T> argumentType, @Nullable CommandArgument.Converter<T> converter);
}
