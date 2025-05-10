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

package team.idealstate.minecraft.next.common.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Supplier;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

public interface Logger {

    void println(@NotNull LogLevel level, @NotNull Supplier<String> messageProvider);

    default void println(@NotNull LogLevel level, String message) {
        println(level, () -> message);
    }

    default void trace(Supplier<String> messageProvider) {
        LogLevel level = LogLevel.TRACE;
        if (Log.getLevel().getIndex() > level.getIndex()) {
            return;
        }
        println(level, messageProvider.get());
    }

    default void trace(String message) {
        trace(() -> message);
    }

    default void debug(Supplier<String> messageProvider) {
        LogLevel level = LogLevel.DEBUG;
        if (Log.getLevel().getIndex() > level.getIndex()) {
            return;
        }
        println(level, messageProvider.get());
    }

    default void debug(String message) {
        debug(() -> message);
    }

    default void info(Supplier<String> messageProvider) {
        LogLevel level = LogLevel.INFO;
        if (Log.getLevel().getIndex() > level.getIndex()) {
            return;
        }
        println(level, messageProvider.get());
    }

    default void info(String message) {
        info(() -> message);
    }

    default void warn(Supplier<String> messageProvider) {
        LogLevel level = LogLevel.WARN;
        if (Log.getLevel().getIndex() > level.getIndex()) {
            return;
        }
        println(level, messageProvider.get());
    }

    default void warn(String message) {
        warn(() -> message);
    }

    default void error(Supplier<String> messageProvider) {
        LogLevel level = LogLevel.ERROR;
        if (Log.getLevel().getIndex() > level.getIndex()) {
            return;
        }
        println(level, messageProvider.get());
    }

    default void error(String message) {
        error(() -> message);
    }

    default void error(Throwable throwable) {
        LogLevel level = LogLevel.ERROR;
        if (Log.getLevel().getIndex() > level.getIndex()) {
            return;
        }

        StringWriter stackTrace = new StringWriter(1024);
        try (PrintWriter it = new PrintWriter(stackTrace)) {
            throwable.printStackTrace(it);
        }
        String message = throwable.getMessage();
        println(level, message + "\n" + stackTrace);
    }

    default void fatal(Supplier<String> messageProvider) {
        LogLevel level = LogLevel.FATAL;
        if (Log.getLevel().getIndex() > level.getIndex()) {
            return;
        }
        println(level, messageProvider.get());
    }

    default void fatal(String message) {
        fatal(() -> message);
    }

    default void fatal(Throwable throwable) {
        LogLevel level = LogLevel.FATAL;
        if (Log.getLevel().getIndex() > level.getIndex()) {
            return;
        }
        StringWriter stackTrace = new StringWriter(1024);
        try (PrintWriter it = new PrintWriter(stackTrace)) {
            throwable.printStackTrace(it);
        }
        String message = throwable.getMessage();
        println(level, message + "\n" + stackTrace);
    }
}
