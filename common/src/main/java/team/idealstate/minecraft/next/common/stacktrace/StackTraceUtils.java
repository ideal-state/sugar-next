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

package team.idealstate.minecraft.next.common.stacktrace;

import java.io.PrintWriter;
import java.io.StringWriter;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

public abstract class StackTraceUtils {

    @NotNull public static String makeDetails(@NotNull Throwable throwable) {
        Validation.notNull(throwable, "throwable must not be null.");
        StringWriter stackTrace = new StringWriter(1024);
        try (PrintWriter it = new PrintWriter(stackTrace)) {
            throwable.printStackTrace(it);
        }
        String message = throwable.getMessage();
        message = message == null ? "" : message;
        return message + "\n" + stackTrace;
    }
}
