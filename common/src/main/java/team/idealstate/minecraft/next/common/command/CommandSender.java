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

import java.util.UUID;
import team.idealstate.minecraft.next.common.validation.annotation.NotNull;

public interface CommandSender {

    UUID CONSOLE_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    /**
     * @return 命令发送者的唯一序列号，可能为 {@link #CONSOLE_UUID}
     */
    @NotNull UUID getUniqueId();

    default boolean isConsole() {
        return CONSOLE_UUID.equals(getUniqueId());
    }

    boolean isAdministrator();

    boolean hasPermission(@NotNull String permission);
}
