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

import java.util.UUID;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Entity;
import org.bukkit.permissions.Permissible;
import team.idealstate.minecraft.next.common.command.CommandSender;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class SpigotCommandSender implements CommandSender {

    @NonNull private final Permissible holder;

    public static SpigotCommandSender of(@NotNull Permissible holder) {
        Validation.notNull(holder, "holder must not be null.");
        return new SpigotCommandSender(holder);
    }

    @Override
    public @NotNull UUID getUniqueId() {
        if (holder instanceof Entity) {
            return ((Entity) holder).getUniqueId();
        }
        return CONSOLE_UUID;
    }

    @Override
    public boolean isAdministrator() {
        return holder.isOp();
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        Validation.notNull(permission, "permission must not be null.");
        return holder.hasPermission(permission);
    }
}
