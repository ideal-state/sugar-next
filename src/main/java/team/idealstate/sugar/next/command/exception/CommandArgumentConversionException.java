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

package team.idealstate.sugar.next.command.exception;

public class CommandArgumentConversionException extends CommandException {
    private static final long serialVersionUID = -2974372242057848514L;

    public CommandArgumentConversionException() {}

    public CommandArgumentConversionException(String message) {
        super(message);
    }

    public CommandArgumentConversionException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommandArgumentConversionException(Throwable cause) {
        super(cause);
    }

    protected CommandArgumentConversionException(
            String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
