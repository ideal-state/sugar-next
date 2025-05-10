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

import java.util.function.Supplier;
import org.slf4j.LoggerFactory;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

public class Slf4JLogger implements Logger {

    private static volatile Slf4JLogger INSTANCE;
    private final org.slf4j.Logger log;

    public Slf4JLogger(Class<?> location) {
        this.log = LoggerFactory.getLogger(location);
    }

    public static Slf4JLogger instance() {
        if (INSTANCE == null) {
            synchronized (Slf4JLogger.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Slf4JLogger(Slf4JLogger.class);
                }
            }
        }
        return INSTANCE;
    }

    @Override
    public void println(@NotNull LogLevel level, @NotNull Supplier<String> messageProvider) {
        int index = level.getIndex();
        if (Log.getLevel().getIndex() > index) {
            return;
        }
        if (index >= LogLevel.ERROR.getIndex()) {
            log.error(messageProvider.get());
        } else {
            log.info(messageProvider.get());
        }
    }

    @Override
    public void trace(Supplier<String> messageProvider) {
        log.trace(messageProvider.get());
    }

    @Override
    public void debug(Supplier<String> messageProvider) {
        log.debug(messageProvider.get());
    }

    @Override
    public void info(Supplier<String> messageProvider) {
        log.info(messageProvider.get());
    }

    @Override
    public void warn(Supplier<String> messageProvider) {
        log.warn(messageProvider.get());
    }

    @Override
    public void error(Supplier<String> messageProvider) {
        log.error(messageProvider.get());
    }

    @Override
    public void fatal(Supplier<String> messageProvider) {
        log.error(messageProvider.get());
    }

    @Override
    public void error(Throwable throwable) {
        String message = throwable.getMessage();
        log.error(message == null ? "" : message, throwable);
    }

    @Override
    public void fatal(Throwable throwable) {
        String message = throwable.getMessage();
        log.error(message == null ? "" : message, throwable);
    }
}
