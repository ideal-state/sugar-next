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
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

public class SystemLogger implements Logger {

    private static volatile SystemLogger INSTANCE;

    public static SystemLogger instance() {
        if (INSTANCE == null) {
            synchronized (SystemLogger.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SystemLogger();
                }
            }
        }
        return INSTANCE;
    }

    private SystemLogger() {}

    @Override
    public void println(@NotNull LogLevel level, @NotNull Supplier<String> messageProvider) {
        int index = level.getIndex();
        if (Log.getLevel().getIndex() > index) {
            return;
        }
        if (index >= LogLevel.ERROR.getIndex()) {
            System.err.println(messageProvider.get());
        } else {
            System.out.println(messageProvider.get());
        }
    }
}
