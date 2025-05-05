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
import team.idealstate.minecraft.next.common.service.ServiceLoader;
import team.idealstate.minecraft.next.common.validate.Validation;

public abstract class Log {

    private static final String LOG_LEVEL_KEY = "k.log.level";
    private static volatile Logger GLOBAL = null;
    private static volatile LogLevel LEVEL;

    static {
        String logLevel = System.getProperty(LOG_LEVEL_KEY);
        if (logLevel == null) {
            LEVEL = LogLevel.INFO;
        } else {
            try {
                LEVEL = LogLevel.valueOf(logLevel);
            } catch (IllegalArgumentException ignored) {
                LEVEL = LogLevel.INFO;
            }
        }
    }

    public static Logger getLogger() {
        if (GLOBAL == null) {
            synchronized (Log.class) {
                if (GLOBAL == null) {
                    GLOBAL =
                            ServiceLoader.singleton(
                                    Logger.class,
                                    Logger.class.getClassLoader(),
                                    () -> {
                                        Logger logger = null;
                                        try {
                                            logger = Log4JLogger.instance();
                                        } catch (NoClassDefFoundError ignored) {
                                        }
                                        try {
                                            logger = Slf4JLogger.instance();
                                        } catch (NoClassDefFoundError ignored) {
                                        }
                                        if (logger == null) {
                                            throw new IllegalStateException(
                                                    "No logger implementation found.");
                                        }
                                        return logger;
                                    });
                }
            }
        }
        return GLOBAL;
    }

    public static LogLevel getLevel() {
        return LEVEL;
    }

    public static void setLevel(LogLevel level) {
        LEVEL = Validation.requireNotNull(level, "Log level cannot be null.");
    }

    public static void println(Supplier<String> messageProvider) {
        println(getLogger(), messageProvider);
    }

    public static void println(Logger logger, Supplier<String> messageProvider) {
        logger.println(messageProvider);
    }

    public static void println(String message) {
        println(getLogger(), message);
    }

    public static void println(Logger logger, String message) {
        logger.println(message);
    }

    public static void trace(Supplier<String> messageProvider) {
        trace(getLogger(), messageProvider);
    }

    public static void trace(Logger logger, Supplier<String> messageProvider) {
        logger.trace(messageProvider);
    }

    public static void trace(String message) {
        trace(getLogger(), message);
    }

    public static void trace(Logger logger, String message) {
        logger.trace(message);
    }

    public static void debug(Supplier<String> messageProvider) {
        debug(getLogger(), messageProvider);
    }

    public static void debug(Logger logger, Supplier<String> messageProvider) {
        logger.debug(messageProvider);
    }

    public static void debug(String message) {
        debug(getLogger(), message);
    }

    public static void debug(Logger logger, String message) {
        logger.debug(message);
    }

    public static void info(Supplier<String> messageProvider) {
        info(getLogger(), messageProvider);
    }

    public static void info(Logger logger, Supplier<String> messageProvider) {
        logger.info(messageProvider);
    }

    public static void info(String message) {
        info(getLogger(), message);
    }

    public static void info(Logger logger, String message) {
        logger.info(message);
    }

    public static void warn(Supplier<String> messageProvider) {
        warn(getLogger(), messageProvider);
    }

    public static void warn(Logger logger, Supplier<String> messageProvider) {
        logger.warn(messageProvider);
    }

    public static void warn(String message) {
        warn(getLogger(), message);
    }

    public static void warn(Logger logger, String message) {
        logger.warn(message);
    }

    public static void error(Supplier<String> messageProvider) {
        error(getLogger(), messageProvider);
    }

    public static void error(Logger logger, Supplier<String> messageProvider) {
        logger.error(messageProvider);
    }

    public static void error(String message) {
        error(getLogger(), message);
    }

    public static void error(Logger logger, String message) {
        logger.error(message);
    }

    public static void error(Throwable throwable) {
        error(getLogger(), throwable);
    }

    public static void error(Logger logger, Throwable throwable) {
        logger.error(throwable);
    }

    public static void fatal(Supplier<String> messageProvider) {
        fatal(getLogger(), messageProvider);
    }

    public static void fatal(Logger logger, Supplier<String> messageProvider) {
        logger.fatal(messageProvider);
    }

    public static void fatal(String message) {
        fatal(getLogger(), message);
    }

    public static void fatal(Logger logger, String message) {
        logger.fatal(message);
    }

    public static void fatal(Throwable throwable) {
        fatal(getLogger(), throwable);
    }

    public static void fatal(Logger logger, Throwable throwable) {
        logger.fatal(throwable);
    }
}
