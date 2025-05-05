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
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import team.idealstate.minecraft.next.common.command.annotation.CommandArgument;
import team.idealstate.minecraft.next.common.command.exception.CommandException;
import team.idealstate.minecraft.next.common.validation.annotation.NotNull;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class SimpleCommandArgumentCompleter implements CommandArgument.Completer {

    @NonNull private final Object command;
    @NonNull private final Method method;

    @Override
    @SuppressWarnings({"unchecked"})
    public @NotNull List<String> complete(
            @NotNull CommandContext context, @NotNull String argument) {
        try {
            List<String> result = (List<String>) method.invoke(command, context, argument);
            return Objects.requireNonNull(result, "completer result cannot be null.");
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new CommandException(e);
        }
    }
}
