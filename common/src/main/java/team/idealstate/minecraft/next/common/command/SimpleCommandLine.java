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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import team.idealstate.minecraft.next.common.command.annotation.CommandArgument;
import team.idealstate.minecraft.next.common.command.annotation.CommandHandler;
import team.idealstate.minecraft.next.common.command.exception.CommandException;
import team.idealstate.minecraft.next.common.logging.Log;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class SimpleCommandLine implements CommandLine {

    public static final int ROOT_DEPTH = -1;
    public static final String ARGUMENTS_DELIMITER = " ";
    public static final String PERMISSION_DELIMITER = ".";
    private final int depth;
    @NonNull private final String name;
    @NonNull private final List<String> permission;
    private final boolean open;
    private final CommandArgument.Converter<?> converter;
    private final CommandArgument.Completer completer;
    private final Deque<SimpleCommandLine> children = new ArrayDeque<>();
    private CommandExecutor executor;

    @NotNull @SuppressWarnings({"rawtypes"})
    public static SimpleCommandLine of(@NotNull String name, @NotNull Object command) {
        CommandLine.validateName(name);
        Validation.notNull(command, "command must not be null.");
        final AtomicReference<SimpleCommandLine> lazyRoot = new AtomicReference<>();
        CommandArgument.Completer completer =
                (context, argument) -> {
                    SimpleCommandLine commandLine = lazyRoot.get();
                    Deque<SimpleCommandLine> children = new ArrayDeque<>(commandLine.children);
                    if (children.isEmpty()) {
                        return Collections.emptyList();
                    }
                    List<String> ret = new ArrayList<>(children.size());
                    CommandSender sender = context.getSender();
                    for (SimpleCommandLine child : children) {
                        if (!validate(sender, child.permission, child.isOpen())) {
                            continue;
                        }
                        CommandArgument.Completer childCompleter = child.completer;
                        if (childCompleter != null) {
                            ret.addAll(childCompleter.complete(context, argument));
                        }
                    }
                    return ret;
                };
        SimpleCommandLine root =
                new SimpleCommandLine(
                        ROOT_DEPTH, name, Collections.singletonList(name), true, null, completer);
        lazyRoot.set(root);
        completer = null;
        Class<?> commandClass = command.getClass();
        Method[] methods = commandClass.getMethods();
        if (methods.length == 0) {
            return root;
        }
        methods =
                Arrays.stream(methods)
                        .sorted(Comparator.comparingInt(Method::getParameterCount))
                        .toArray(Method[]::new);
        for (Method method : methods) {
            if (Modifier.isStatic(method.getModifiers())
                    || !CommandResult.class.equals(method.getReturnType())) {
                continue;
            }
            CommandHandler commandHandler = method.getDeclaredAnnotation(CommandHandler.class);
            if (commandHandler == null) {
                continue;
            }
            String value = commandHandler.value();
            String methodName = method.getName();
            if (value.isEmpty()) {
                value = methodName;
            }
            String[] arguments = value.split(ARGUMENTS_DELIMITER);
            int variableCount = 0;
            for (int i = 0; i < arguments.length; i++) {
                String argument = arguments[i];
                int last = argument.length() - 1;
                if (argument.charAt(0) == '{' && argument.charAt(last) == '}') {
                    argument = argument.substring(1, last);
                    arguments[i] = argument;
                    variableCount++;
                } else {
                    if (variableCount > 0) {
                        throw new IllegalArgumentException(
                                "literal argument must before to variable argument.");
                    }
                }
                CommandLine.validateName(argument, "argument");
            }
            String[] permission = commandHandler.permission();
            if (permission.length == 0) {
                permission = arguments;
            }
            Parameter[] parameters = method.getParameters();
            Map<String, CommandArgument> commandArguments = new HashMap<>(parameters.length);
            for (Parameter parameter : parameters) {
                Log.debug(
                        () ->
                                String.format(
                                        "%s(...): parameter '%s'",
                                        methodName, parameter.getName()));
                CommandArgument commandArgument =
                        parameter.getDeclaredAnnotation(CommandArgument.class);
                Class<?> parameterType = parameter.getType();
                if (commandArgument == null) {
                    if (CommandContext.class.equals(parameterType)) {
                        continue;
                    }
                    throw new IllegalArgumentException(
                            "parameter must be a CommandContext or annotated with"
                                    + " @CommandArgument.");
                }
                value = commandArgument.value();
                if (value.isEmpty()) {
                    value = parameter.getName();
                }
                commandArguments.put(value, commandArgument);
            }
            if (commandArguments.size() != variableCount) {
                throw new IllegalArgumentException("parameter size must be same as variable size.");
            }
            SimpleCommandLine parent = root;
            int variableStart = arguments.length - variableCount;
            for (int i = 0; i < arguments.length; i++) {
                String childName = arguments[i];
                CommandArgument.Converter<?> converter = null;
                completer = null;
                boolean isVariable = i >= variableStart;
                if (isVariable) {
                    CommandArgument commandArgument = commandArguments.get(childName);
                    if (commandArgument == null) {
                        throw new IllegalArgumentException(
                                String.format(
                                        "%s(...): variable parameter '%s' must be annotated with"
                                                + " @CommandArgument.",
                                        methodName, childName));
                    }
                    try {
                        Class<? extends CommandArgument.Converter> converterClass =
                                commandArgument.converter();
                        if (!CommandArgument.Converter.class.equals(converterClass)) {
                            Constructor<? extends CommandArgument.Converter> constructor =
                                    converterClass.getConstructor();
                            converter = constructor.newInstance();
                        } else {
                            converter = SimpleCommandArgumentConverter.INSTANCE;
                        }
                        Class<? extends CommandArgument.Completer> completerClass =
                                commandArgument.completer();
                        if (!CommandArgument.Completer.class.equals(completerClass)) {
                            Constructor<? extends CommandArgument.Completer> constructor =
                                    completerClass.getConstructor();
                            completer = constructor.newInstance();
                        } else {
                            String completerMethodName = commandArgument.completerMethod();
                            if (!completerMethodName.isEmpty()) {
                                Method completerMethod =
                                        commandClass.getMethod(
                                                completerMethodName,
                                                CommandContext.class,
                                                String.class);
                                if (!Modifier.isStatic(completerMethod.getModifiers())) {
                                    completerMethod.setAccessible(true);
                                    completer =
                                            new SimpleCommandArgumentCompleter(
                                                    command, completerMethod);
                                }
                            }
                        }
                    } catch (ReflectiveOperationException e) {
                        throw new CommandException(e);
                    }
                }
                if (!isVariable) {
                    List<String> list = Collections.singletonList(childName);
                    completer =
                            (context, argument) -> {
                                if (argument.isEmpty()
                                        || childName
                                                .toLowerCase()
                                                .startsWith(argument.toLowerCase())) {
                                    return list;
                                }
                                return Collections.emptyList();
                            };
                }
                parent =
                        parent.addChild(
                                childName,
                                Arrays.asList(permission),
                                commandHandler.open(),
                                converter,
                                completer);
            }
            method.setAccessible(true);
            parent.executor = new SimpleCommandExecutor(command, method);
        }
        return root;
    }

    public static String permissionOf(@NotNull List<String> permissionNodes) {
        Validation.notNull(permissionNodes, "permissionNodes must not be null.");
        if (permissionNodes.isEmpty()) {
            return "";
        }
        for (String permissionNode : permissionNodes) {
            CommandLine.validateName(permissionNode, "permissionNode");
        }
        if (permissionNodes.size() == 1) {
            return permissionNodes.get(0);
        }
        return String.join(PERMISSION_DELIMITER, permissionNodes);
    }

    @NotNull private static List<SimpleCommandLine> accept(
            @NotNull SimpleCommandLine parent,
            @NotNull CommandContext context,
            int current,
            @NotNull String... arguments) {
        int next = current + 1;
        if (next >= arguments.length) {
            return Collections.emptyList();
        }
        Deque<SimpleCommandLine> children = new ArrayDeque<>(parent.children);
        if (children.isEmpty()) {
            return Collections.emptyList();
        }
        String argument = arguments[next];
        Map<Integer, List<SimpleCommandLine>> accepted = new HashMap<>(children.size());
        for (SimpleCommandLine child : children) {
            if (child.depth != next || !child.accept(context, argument)) {
                continue;
            }
            int count = 0;
            List<SimpleCommandLine> acceptedChildren = new ArrayList<>();
            List<SimpleCommandLine> nextAcceptedChildren = Collections.singletonList(child);
            do {
                count++;
                acceptedChildren.addAll(nextAcceptedChildren);
                nextAcceptedChildren =
                        accept(
                                acceptedChildren.get(acceptedChildren.size() - 1),
                                context,
                                next,
                                arguments);
            } while (!nextAcceptedChildren.isEmpty());
            if (!accepted.containsKey(count)) {
                accepted.put(count, acceptedChildren);
            }
        }
        if (accepted.isEmpty()) {
            return Collections.emptyList();
        }
        Integer key = accepted.keySet().stream().max(Integer::compare).get();
        return accepted.get(key);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean validate(@NotNull CommandContext context, @NotNull String... arguments) {
        Validation.notNull(context, "context must not be null.");
        Validation.notNull(arguments, "arguments must not be null.");
        if (arguments.length == 0) {
            return false;
        }
        for (String argument : arguments) {
            Validation.notNull(argument, "argument must not be null.");
        }
        return true;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean validate(
            @NotNull CommandSender sender, @NotNull List<String> permissionNodes, boolean open) {
        Validation.notNull(sender, "sender must not be null.");
        Validation.notNull(permissionNodes, "permissionNodes must not be null.");
        if (open || sender.isAdministrator()) {
            return true;
        }
        String permission = permissionOf(permissionNodes);
        if (permission.isEmpty()) {
            return true;
        }
        return sender.hasPermission(permission);
    }

    @NotNull private SimpleCommandLine addChild(
            @NotNull String name,
            @NotNull List<String> permission,
            boolean open,
            CommandArgument.Converter<?> converter,
            CommandArgument.Completer completer) {
        Validation.notNull(name, "name must not be null.");
        Validation.notNull(permission, "permission must not be null.");
        SimpleCommandLine child =
                new SimpleCommandLine(depth + 1, name, permission, open, converter, completer);
        children.add(child);
        return child;
    }

    private boolean accept(@NotNull CommandContext context, @NotNull String argument) {
        return converter == null
                ? getName().equalsIgnoreCase(argument)
                : converter.canCovert(context, argument);
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull String[] getPermission() {
        return permission.toArray(new String[0]);
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public @NotNull CommandResult execute(
            @NotNull CommandContext context, @NotNull String... arguments) throws CommandException {
        if (!validate(context, arguments)) {
            return CommandResult.failure();
        }
        List<SimpleCommandLine> acceptedChildren = accept(this, context, depth, arguments);
        if (acceptedChildren.isEmpty()) {
            return CommandResult.failure();
        }
        SimpleCommandLine accepted = acceptedChildren.get(acceptedChildren.size() - 1);
        if (accepted == null) {
            return CommandResult.failure();
        }
        CommandExecutor executor = accepted.executor;
        if (executor == null) {
            return CommandResult.failure();
        }
        if (!validate(context.getSender(), accepted.permission, accepted.open)) {
            return CommandResult.failure();
        }
        for (SimpleCommandLine acceptedChild : acceptedChildren) {
            CommandArgument.Converter<?> converter = acceptedChild.converter;
            if (converter == null) {
                continue;
            }
            Object argument = converter.convert(context, arguments[acceptedChild.depth]);
            context.put(acceptedChild.getName(), argument);
        }
        int depth = accepted.depth;
        return executor.execute(context, depth, arguments);
    }

    @Override
    public @NotNull List<String> complete(
            @NotNull CommandContext context, @NotNull String... arguments) throws CommandException {
        if (!validate(context, arguments)) {
            return Collections.emptyList();
        }
        List<SimpleCommandLine> acceptedChildren = accept(this, context, depth, arguments);
        List<String> completed;
        if (acceptedChildren.isEmpty()) {
            if (arguments.length > 1 || completer == null) {
                return Collections.emptyList();
            }
            completed = completer.complete(context, arguments[0]);
        } else {
            SimpleCommandLine accepted = acceptedChildren.get(acceptedChildren.size() - 1);
            if (accepted == null) {
                return Collections.emptyList();
            }
            CommandArgument.Completer completer = accepted.completer;
            if (completer == null) {
                return Collections.emptyList();
            }
            if (!validate(context.getSender(), accepted.permission, accepted.open)) {
                return Collections.emptyList();
            }
            int depth = accepted.depth;
            if (arguments.length - 1 != depth) {
                return Collections.emptyList();
            }
            completed = completer.complete(context, arguments[depth]);
        }
        if (completed.isEmpty()) {
            return completed;
        }
        Set<String> set = new LinkedHashSet<>(completed);
        return new ArrayList<>(set);
    }
}
