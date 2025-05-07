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

package team.idealstate.minecraft.next.common.command.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.UUID;
import lombok.Data;
import org.junit.jupiter.api.Test;
import team.idealstate.minecraft.next.common.command.CommandContext;
import team.idealstate.minecraft.next.common.command.CommandLine;
import team.idealstate.minecraft.next.common.command.CommandResult;
import team.idealstate.minecraft.next.common.command.CommandSender;
import team.idealstate.minecraft.next.common.command.example.ExampleCommand;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

public class CommandTest {

    @Test
    public void test() {
        TestCommandSender sender =
                new TestCommandSender(UUID.randomUUID(), true, Set.of("next.command.sum"));
        ExampleCommand command = new ExampleCommand();
        CommandLine commandLine = CommandLine.of("test", command);
        String[] arguments = "sum 1 2 3".split(" ", -1);
        CommandContext context = CommandContext.of(sender);
        CommandResult result = commandLine.execute(context, arguments);
        System.out.printf("Command result: %s%n", result);
        assertTrue(result.isSuccess());
    }

    @SuppressWarnings("ClassCanBeRecord")
    @Data
    public static final class TestCommandSender implements CommandSender {

        private final @NotNull UUID uniqueId;
        private final boolean administrator;
        private final Set<String> permissions;

        @Override
        public boolean hasPermission(@NotNull String permission) {
            return permissions.contains(permission);
        }
    }
}
