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

package team.idealstate.sugar.next.command;

import java.util.List;
import team.idealstate.sugar.next.command.exception.CommandException;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;

public interface CommandLine {

    @NotNull
    static CommandLine of(@NotNull String name, @NotNull Object command) {
        Validation.notNull(name, "name must not be null.");
        Validation.notNull(command, "command must not be null.");
        return SimpleCommandLine.of(name, command);
    }

    @NotNull
    static String validateName(@NotNull String name) {
        return validateName(name, "name");
    }

    @NotNull
    static String validateName(@NotNull String name, @NotNull String parameterName) {
        Validation.notNull(name, "name must not be null.");
        boolean header = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (header) {
                if (!(('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z'))) {
                    throw new IllegalArgumentException(parameterName + " must start with a English letter.");
                }
                header = false;
            }
            if (!(('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z') || ('0' <= c && c <= '9'))) {
                throw new IllegalArgumentException(parameterName + " must contain only English letters and numbers.");
            }
        }
        return name;
    }

    @NotNull
    String getName();

    @NotNull
    String[] getPermission();

    boolean isOpen();

    @NotNull
    CommandResult execute(@NotNull CommandContext context, @NotNull String... arguments) throws CommandException;

    @NotNull
    List<String> complete(@NotNull CommandContext context, @NotNull String... arguments) throws CommandException;
}
