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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class CommandResult {

    private boolean success = false;
    private String message = null;

    @NotNull public static CommandResult success() {
        return success(null);
    }

    @NotNull public static CommandResult success(String message) {
        return new CommandResult(true, message);
    }

    @NotNull public static CommandResult fail() {
        return fail(null);
    }

    @NotNull public static CommandResult fail(String message) {
        return new CommandResult(false, message);
    }
}
