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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import team.idealstate.sugar.next.command.annotation.CommandArgument;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;
import team.idealstate.sugar.validate.annotation.Nullable;

final class SimpleCommandContext extends LinkedHashMap<String, Object> implements CommandContext {
    private static final long serialVersionUID = -1605421926443481109L;
    private final CommandSender sender;

    public SimpleCommandContext(
            @NotNull CommandSender sender, @NotNull Map<? extends String, ?> map, @NotNull String[] arguments) {
        super(Validation.requireNotNull(map, "Map must not be null."));
        Validation.notNull(sender, "Sender must not be null.");
        Validation.notNull(arguments, "Arguments must not be null.");
        this.sender = sender;
        this.arguments = new String[arguments.length];
        if (arguments.length != 0) {
            System.arraycopy(arguments, 0, this.arguments, 0, arguments.length);
        }
    }

    @Override
    public @NotNull CommandSender getSender() {
        return sender;
    }

    private final String[] arguments;

    @NotNull
    @Override
    public List<String> getArguments() {
        return Arrays.asList(arguments);
    }

    private final Map<Class<?>, CommandArgument.Completer> completers = new ConcurrentHashMap<>();

    @Nullable
    @Override
    public CommandArgument.Completer getCompleter(@NotNull Class<?> argumentType) {
        Validation.notNull(argumentType, "argumentType must not be null.");
        return completers.get(argumentType);
    }

    @Override
    public void setCompleter(@NotNull Class<?> argumentType, @Nullable CommandArgument.Completer completer) {
        Validation.notNull(argumentType, "argumentType must not be null.");
        if (completer == null) {
            completers.remove(argumentType);
        } else {
            completers.put(argumentType, completer);
        }
    }

    private final Map<Class<?>, CommandArgument.Converter<?>> converters = new ConcurrentHashMap<>();

    @Nullable
    @Override
    @SuppressWarnings({"unchecked"})
    public <T> CommandArgument.Converter<T> getConverter(@NotNull Class<T> argumentType) {
        Validation.notNull(argumentType, "argumentType must not be null.");
        return (CommandArgument.Converter<T>) converters.get(argumentType);
    }

    @Override
    public <T> void setConverter(@NotNull Class<T> argumentType, @Nullable CommandArgument.Converter<T> converter) {
        Validation.notNull(argumentType, "argumentType must not be null.");
        if (converter == null) {
            converters.remove(argumentType);
        } else {
            converters.put(argumentType, converter);
        }
    }
}
