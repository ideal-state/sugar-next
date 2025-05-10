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

package team.idealstate.sugar.next.calculate.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
public class ExpressionCalculationException extends ExpressionException {
    private static final long serialVersionUID = -2384400047561006816L;

    private final String expression;

    public ExpressionCalculationException(String expression, String message) {
        super(makeMessage(expression, message));
        this.expression = expression;
    }

    public ExpressionCalculationException(String expression, String message, Throwable cause) {
        super(makeMessage(expression, message), cause);
        this.expression = expression;
    }

    public ExpressionCalculationException(String expression, Throwable cause) {
        super(makeMessage(expression), cause);
        this.expression = expression;
    }

    protected ExpressionCalculationException(
            String expression, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(makeMessage(expression, message), cause, enableSuppression, writableStackTrace);
        this.expression = expression;
    }

    private static String makeMessage(String expression) {
        return makeMessage(expression, "");
    }

    private static String makeMessage(String expression, String message) {
        return message + "(" + expression + ")";
    }
}
