package team.idealstate.minecraft.next.common.command.test;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import team.idealstate.minecraft.next.common.command.CommandContext;
import team.idealstate.minecraft.next.common.command.CommandLine;
import team.idealstate.minecraft.next.common.command.CommandResult;
import team.idealstate.minecraft.next.common.command.CommandSender;
import team.idealstate.minecraft.next.common.command.example.ExampleCommand;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CommandTest {

    @Test
    public void test() {
        TestCommandSender sender = new TestCommandSender(UUID.randomUUID(), true, Set.of("next.command.sum"));
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
