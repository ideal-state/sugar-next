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

package team.idealstate.sugar.next.calculate;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import team.idealstate.sugar.next.calculate.exception.ExpressionCalculationException;
import team.idealstate.sugar.next.calculate.exception.ExpressionException;
import team.idealstate.sugar.next.calculate.exception.ExpressionSyntaxException;
import team.idealstate.sugar.next.calculate.operation.Operator;
import team.idealstate.sugar.next.calculate.operation.standard.StandardOperator;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;

/** 轻量安全且快速的预编译数值表达式 */
@EqualsAndHashCode
@ToString
public final class Expression implements Cloneable {

    @Getter
    private final String expression;

    private final Operator[] operators;

    private final transient Object lock = new Object();
    private transient volatile List<Token> compiled;

    public Expression(@NotNull String expression) {
        this(expression, StandardOperator.ALL);
    }

    public Expression(@NotNull String expression, @NotNull Collection<? extends Operator> operators) {
        Validation.notBlank(expression, "expression must not be blank.");
        this.expression = expression;
        this.operators = operators.toArray(new Operator[0]);
    }

    private boolean isCompiled() {
        return compiled != null;
    }

    /**
     * 编译表达式
     *
     * @return this
     * @throws ExpressionSyntaxException
     */
    @NotNull
    public Expression compile() throws ExpressionSyntaxException {
        if (isCompiled()) {
            return this;
        }
        synchronized (lock) {
            if (isCompiled()) {
                return this;
            }
            this.compiled = Collections.unmodifiableList(new Lexer(expression, operators).rpn());
        }
        return this;
    }

    /**
     * 使用指定变量上下文的计算
     *
     * @param context 变量上下文
     * @return 计算结果
     * @throws ExpressionException
     */
    @NotNull
    public Number calculate(@NotNull Map<String, Number> context) throws ExpressionException {
        Validation.notNull(context, "Context must not be null.");
        compile();
        int size = compiled.size();
        Deque<Number> operationStack = new ArrayDeque<>(size);
        for (Token token : compiled) {
            TokenType object = token.getType();
            if (TokenType.NUMBER.equals(object)) {
                operationStack.push((Number) token.getValue());
            } else if (TokenType.IDENTIFIER.equals(object)) {
                Number number = context.get(token.getLiteral());
                if (number == null) {
                    throw new ExpressionCalculationException(
                            expression, "Invalid variable value! (" + token.getLiteral() + "=null)");
                }
                operationStack.push(number);
            } else if (TokenType.OPERATOR.equals(object)) {
                Operator operator = (Operator) token.getValue();
                int arity = operator.getArity().getArity();
                Number[] operands = new Number[arity];
                for (int i = arity; i > 0; i--) {
                    if (operationStack.peek() == null) {
                        throw new ExpressionCalculationException(expression, "Invalid expression!");
                    }
                    operands[i - 1] = operationStack.pop();
                }
                operationStack.push(operator.operate(operands));
            } else {
                throw new ExpressionCalculationException(expression, "Invalid expression!");
            }
        }
        if (operationStack.size() == 1) {
            return operationStack.pop();
        }
        throw new ExpressionCalculationException(expression, "Invalid expression!");
    }

    /**
     * 使用空变量上下文的计算
     *
     * @return 计算结果
     * @throws ExpressionException
     * @see Expression#calculate(Map)
     */
    @NotNull
    public Number calculate() throws ExpressionException {
        return calculate(Collections.emptyMap());
    }

    /**
     * @param context 变量上下文
     * @return 算式结果大于 0 时为 true，否则为 false
     */
    public boolean isTrue(@NotNull Map<String, Number> context) {
        return calculate(context).doubleValue() > 0;
    }

    /** @see Expression#isTrue(Map) */
    public boolean isTrue() {
        return isTrue(Collections.emptyMap());
    }

    /**
     * @param context 变量上下文
     * @return 算式结果小于等于 0 时为 true，否则为 false
     */
    public boolean isFalse(@NotNull Map<String, Number> context) {
        return !isTrue(context);
    }

    /** @see Expression#isFalse(Map) */
    public boolean isFalse() {
        return isFalse(Collections.emptyMap());
    }

    /** @return 深拷贝 */
    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public Expression clone() {
        Expression expression = new Expression(this.expression, Arrays.asList(operators));
        expression.compiled = this.compiled;
        return expression;
    }
}
