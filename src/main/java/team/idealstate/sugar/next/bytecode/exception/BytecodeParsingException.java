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

package team.idealstate.sugar.next.bytecode.exception;

public class BytecodeParsingException extends BytecodeException {
    private static final long serialVersionUID = 4850014410123133297L;

    public BytecodeParsingException() {}

    public BytecodeParsingException(String message) {
        super(message);
    }

    public BytecodeParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    public BytecodeParsingException(Throwable cause) {
        super(cause);
    }

    protected BytecodeParsingException(
            String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
