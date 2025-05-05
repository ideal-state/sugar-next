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

package team.idealstate.minecraft.next.common.calculate.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
public class ExpressionSyntaxException extends ExpressionException {
    private static final long serialVersionUID = 5327302324147703837L;

    private final String expression;
    private final int line;
    private final int column;

    public ExpressionSyntaxException(String expression, int line, int column, String message) {
        super(makeMessage(expression, line, column, message));
        this.expression = expression;
        this.line = line;
        this.column = column;
    }

    public ExpressionSyntaxException(
            String expression, int line, int column, String message, Throwable cause) {
        super(makeMessage(expression, line, column, message), cause);
        this.expression = expression;
        this.line = line;
        this.column = column;
    }

    public ExpressionSyntaxException(String expression, int line, int column, Throwable cause) {
        super(makeMessage(expression, line, column), cause);
        this.expression = expression;
        this.line = line;
        this.column = column;
    }

    protected ExpressionSyntaxException(
            String expression,
            int line,
            int column,
            String message,
            Throwable cause,
            boolean enableSuppression,
            boolean writableStackTrace) {
        super(
                makeMessage(expression, line, column, message),
                cause,
                enableSuppression,
                writableStackTrace);
        this.expression = expression;
        this.line = line;
        this.column = column;
    }

    private static String makeMessage(String expression, int line, int column) {
        return makeMessage(expression, line, column, "");
    }

    private static String makeMessage(String expression, int line, int column, String message) {
        return message + "(\"" + expression + "\" at line " + line + " column " + column + ')';
    }
}
