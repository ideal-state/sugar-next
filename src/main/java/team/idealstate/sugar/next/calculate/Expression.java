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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import team.idealstate.sugar.next.calculate.exception.ExpressionCalculationException;
import team.idealstate.sugar.next.calculate.exception.ExpressionException;
import team.idealstate.sugar.next.calculate.exception.ExpressionSyntaxException;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;

/** 轻量安全且快速的预编译数值表达式 */
@EqualsAndHashCode
@ToString
public final class Expression implements Cloneable {
    private static final char MINUS_SIGN = '-';
    private static final char DECIMAL_POINT = '.';

    @Getter
    private final String expression;

    private final Map<String, Operator> operatorTable;
    private final transient Object lock = new Object();
    private transient volatile List<Object> compiled;

    public Expression(String expression) {
        this(expression, Arrays.asList(Operator.values()));
    }

    public Expression(String expression, Collection<? extends Operator> operators) {
        validateExpression(expression);
        this.expression = expression;
        this.operatorTable = new LinkedHashMap<>(operators.size());
        operators.forEach(operator -> operatorTable.put(operator.getSymbol(), operator));
    }

    private static void validateExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid expression!");
        }
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
    public Expression compile() throws ExpressionSyntaxException {
        if (isCompiled()) {
            return this;
        }
        synchronized (lock) {
            if (isCompiled()) {
                return this;
            }
            final int length = expression.length();
            Deque<Object> operationStack = new ArrayDeque<>(length);
            Deque<Symbol> symbolStack = new ArrayDeque<>(length);
            StringBuilder symbolBuilder = new StringBuilder(length);
            StringBuilder numberBuilder = new StringBuilder(length);
            StringBuilder variableBuilder = new StringBuilder(length);
            int line = 0;
            int column = 0;
            int lastPriority = Operator.MIN_PRIORITY;
            boolean allowMinusSign = true;
            boolean isDecimal = false;
            for (int i = 0; i < length; i++) {
                char current = expression.charAt(i);
                if (current == '\n') {
                    line++;
                    column = 0;
                    continue;
                }
                if (Character.isWhitespace(current)) {
                    column++;
                    continue;
                }
                if (allowMinusSign && current == MINUS_SIGN && numberBuilder.length() == 0) {
                    numberBuilder.append(current);
                    allowMinusSign = false;
                    column++;
                    continue;
                }
                if (Character.isDigit(current)) {
                    if (variableBuilder.length() > 0) {
                        variableBuilder.append(current);
                    } else {
                        numberBuilder.append(current);
                    }
                    allowMinusSign = false;
                    column++;
                    continue;
                }
                if (current == DECIMAL_POINT) {
                    if (isDecimal
                            || numberBuilder.length() == 0
                            || (numberBuilder.length() == 1 && numberBuilder.charAt(0) == MINUS_SIGN)) {
                        throw new ExpressionSyntaxException(expression, line, column, "Invalid decimal point!");
                    }
                    numberBuilder.append(current);
                    isDecimal = true;
                    allowMinusSign = false;
                    column++;
                    continue;
                }
                symbolBuilder.append(current);
                String symbolStr = symbolBuilder.toString();
                if (Parentheses.LEFT.isSymbol(symbolStr)) {
                    symbolBuilder.setLength(0);
                    symbolStack.push(Parentheses.LEFT);
                    lastPriority = Operator.MIN_PRIORITY;
                    allowMinusSign = true;
                    column++;
                    continue;
                }
                if (Parentheses.RIGHT.isSymbol(symbolStr)) {
                    symbolBuilder.setLength(0);
                    if (symbolStack.isEmpty() || !symbolStack.contains(Parentheses.LEFT)) {
                        throw new ExpressionSyntaxException(expression, line, column, String.format(
                                "Invalid operator '%s'!", Parentheses.RIGHT.getSymbol()
                        ));
                    }
                    Object numericVariable =
                            makeNumericVariable(numberBuilder, variableBuilder, line, column, isDecimal, true);
                    isDecimal = false;
                    if (numericVariable != null) {
                        operationStack.push(numericVariable);
                    }
                    do {
                        Symbol symbol = symbolStack.pop();
                        if (symbol instanceof Operator) {
                            operationStack.push(symbol);
                            continue;
                        }
                        if (Parentheses.LEFT.equals(symbol)) {
                            Symbol lastOperator = symbolStack.peek();
                            if (lastOperator == null || Parentheses.LEFT.equals(lastOperator)) {
                                lastPriority = Operator.MIN_PRIORITY;
                                break;
                            }
                            if (lastOperator instanceof Operator) {
                                lastPriority = ((Operator) lastOperator).getPriority();
                                break;
                            }
                        }
                        throw new ExpressionSyntaxException(expression, line, column, "Invalid operator!");
                    } while (!symbolStack.isEmpty());
                    allowMinusSign = false;
                    column++;
                    continue;
                }
                if (Variable.isNameContent(current)) {
                    symbolBuilder.setLength(0);
                    if (numberBuilder.length() > 0) {
                        throw new ExpressionSyntaxException(expression, line, column, "Invalid number!");
                    }
                    if (variableBuilder.length() == 0 && !Variable.isNameHeader(current)) {
                        throw new ExpressionSyntaxException(expression, line, column, "Invalid variable!");
                    }
                    variableBuilder.append(current);
                    allowMinusSign = false;
                    column++;
                    continue;
                }
                if (Operator.KEYWORDS.contains(current)) {
                    for (int j = i + 1; j < length; j++) {
                        char c = expression.charAt(j);
                        if (Operator.KEYWORDS.contains(c)) {
                            symbolBuilder.append(c);
                            i = j;
                        } else {
                            break;
                        }
                    }
                }
                symbolStr = symbolBuilder.toString();
                symbolBuilder.setLength(0);
                Operator operator = operatorTable.get(symbolStr);
                if (operator == null) {
                    throw new ExpressionSyntaxException(expression, line, column, String.format(
                            "Invalid operator '%s'!", symbolStr
                    ));
                }
                Object numericVariable =
                        makeNumericVariable(numberBuilder, variableBuilder, line, column, isDecimal, true);
                isDecimal = false;
                if (numericVariable != null) {
                    operationStack.push(numericVariable);
                }
                final int currentPriority = operator.getPriority();
                while (currentPriority <= lastPriority && !symbolStack.isEmpty()) {
                    Symbol symbol = symbolStack.pop();
                    if (symbol instanceof Operator) {
                        operationStack.push(symbol);
                        Symbol lastOperator = symbolStack.peek();
                        if (lastOperator == null) {
                            //                            // 冗余操作
                            //                            lastPriority = Operator.MIN_PRIORITY;
                            break;
                        }
                        if (lastOperator instanceof Operator) {
                            lastPriority = ((Operator) lastOperator).getPriority();
                            continue;
                        }
                    }
                    throw new ExpressionSyntaxException(expression, line, column, "Invalid operator!");
                }
                symbolStack.push(operator);
                lastPriority = currentPriority;
                allowMinusSign = true;
                column++;
            }
            column--;
            if (symbolBuilder.length() != 0) {
                throw new ExpressionSyntaxException(expression, line, column, String.format(
                        "Invalid symbol '%s'!", symbolBuilder
                ));
            }
            if (symbolStack.contains(Parentheses.LEFT)) {
                throw new ExpressionSyntaxException(expression, line, column, String.format(
                        "Invalid symbol stack '%s'!", symbolStack
                ));
            }
            Object numericVariable = makeNumericVariable(numberBuilder, variableBuilder, line, column, isDecimal, true);
            if (numericVariable != null) {
                operationStack.push(numericVariable);
            }
            for (Symbol symbol : symbolStack) {
                operationStack.push(symbol);
            }
            symbolStack.clear();
            if (operationStack.isEmpty()) {
                throw new ExpressionSyntaxException(expression, line, column, String.format(
                        "Invalid operation stack '%s'!", operationStack
                ));
            }
            int countNumericVariable = 0;
            int countOperator = 0;
            Iterator<Object> iterator = operationStack.descendingIterator();
            while (iterator.hasNext()) {
                Object symbol = iterator.next();
                if (symbol instanceof Operator) {
                    countOperator++;
                } else if (symbol instanceof Variable || symbol instanceof Number) {
                    countNumericVariable++;
                } else {
                    throw new ExpressionSyntaxException(expression, line, column, String.format(
                            "Invalid symbol '%s'!", symbol
                    ));
                }
            }

            List<Object> result = new ArrayList<>(operationStack);
            Collections.reverse(result);
            if (countNumericVariable != countOperator + 1) {
                throw new ExpressionSyntaxException(expression, line, column, String.format(
                        "Invalid operation stack '%s'!", result
                ));
            }
            this.compiled = Collections.unmodifiableList(result);
        }
        return this;
    }

    private Object makeNumericVariable(
            StringBuilder numberBuilder, StringBuilder variableBuilder, int line, int column, boolean isDecimal) {
        return makeNumericVariable(numberBuilder, variableBuilder, line, column, isDecimal, false);
    }

    private Object makeNumericVariable(
            StringBuilder numberBuilder,
            StringBuilder variableBuilder,
            int line,
            int column,
            boolean isDecimal,
            boolean optional) {
        Object numericVariable = null;
        if (numberBuilder.length() > 0) {
            try {
                String string = numberBuilder.toString();
                if (isDecimal) {
                    try {
                        numericVariable = Double.valueOf(string);
                    } catch (NumberFormatException e) {
                        numericVariable = new BigDecimal(string);
                    }
                } else {
                    try {
                        numericVariable = Integer.valueOf(string);
                    } catch (NumberFormatException e) {
                        numericVariable = new BigInteger(string);
                    }
                }
            } catch (NumberFormatException e) {
                throw new ExpressionSyntaxException(expression, line, column, e);
            }
            numberBuilder.setLength(0);
        } else if (variableBuilder.length() > 0) {
            numericVariable = new Variable(variableBuilder.toString(), null);
            variableBuilder.setLength(0);
        } else if (!optional) {
            throw new ExpressionSyntaxException(expression, line, column, "Invalid numeric variable!");
        }
        return numericVariable;
    }

    /**
     * 使用指定变量上下文的计算
     *
     * @param context 变量上下文
     * @return 计算结果
     * @throws ExpressionException
     */
    public Number calculate(@NotNull Map<String, Number> context) throws ExpressionException {
        Validation.notNull(context, "Context must not be null.");
        compile();
        int size = compiled.size();
        Deque<Number> operationStack = new ArrayDeque<>(size);
        for (Object object : compiled) {
            if (object instanceof Number) {
                operationStack.push((Number) object);
            } else if (object instanceof Variable) {
                Number number = ((Variable) object).valueOf(context);
                if (number == null) {
                    throw new ExpressionCalculationException(
                            expression, "Invalid variable value! (" + ((Variable) object).getName() + "=null)");
                }
                operationStack.push(number);
            } else if (object instanceof Operator) {
                Number second;
                Number first;
                try {
                    second = operationStack.pop();
                    first = operationStack.pop();
                } catch (NoSuchElementException e) {
                    throw new ExpressionCalculationException(expression, "Invalid expression!", e);
                }
                operationStack.push(((Operator) object).calculate(first, second));
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

    /**
     * @see Expression#isTrue(Map)
     */
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

    /**
     * @see Expression#isFalse(Map)
     */
    public boolean isFalse() {
        return isFalse(Collections.emptyMap());
    }

    /** @return 深拷贝 */
    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public Expression clone() {
        Expression expression = new Expression(this.expression, operatorTable.values());
        expression.compiled = Collections.unmodifiableList(this.compiled);
        return expression;
    }
}
