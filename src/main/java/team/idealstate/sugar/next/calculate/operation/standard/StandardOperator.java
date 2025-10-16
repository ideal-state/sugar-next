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

package team.idealstate.sugar.next.calculate.operation.standard;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import team.idealstate.sugar.next.calculate.exception.ExpressionOperationException;
import team.idealstate.sugar.next.calculate.operation.Operable;
import team.idealstate.sugar.next.calculate.operation.Operator;
import team.idealstate.sugar.next.calculate.operation.OperatorArity;
import team.idealstate.sugar.next.calculate.operation.OperatorAssociativity;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class StandardOperator implements Operator, Serializable {

    // 1
    public static final Operator LOGICAL_NOT = new StandardOperator(
            9000, OperatorArity.UNARY, OperatorAssociativity.RIGHT, "!", StandardOperable::logicalNot);
    public static final Operator BITWISE_NOT = new StandardOperator(
            9000, OperatorArity.UNARY, OperatorAssociativity.RIGHT, "~", StandardOperable::bitwiseNot);
    public static final Operator ARITHMETIC_POSITIVE_SIGN = new StandardOperator(
            9000, OperatorArity.UNARY, OperatorAssociativity.RIGHT, "+", StandardOperable::arithmeticPositiveSign);
    public static final Operator ARITHMETIC_MINUS_SIGN = new StandardOperator(
            9000, OperatorArity.UNARY, OperatorAssociativity.RIGHT, "-", StandardOperable::arithmeticMinusSign);

    // 2
    public static final Operator ARITHMETIC_MULTIPLY = new StandardOperator(
            8000, OperatorArity.BINARY, OperatorAssociativity.LEFT, "*", StandardOperable::arithmeticMultiply);
    public static final Operator ARITHMETIC_DIVIDE = new StandardOperator(
            8000, OperatorArity.BINARY, OperatorAssociativity.LEFT, "/", StandardOperable::arithmeticDivide);
    public static final Operator ARITHMETIC_MOD = new StandardOperator(
            8000, OperatorArity.BINARY, OperatorAssociativity.LEFT, "%", StandardOperable::arithmeticMod);
    public static final Operator ARITHMETIC_POWER = new StandardOperator(
            8000, OperatorArity.BINARY, OperatorAssociativity.LEFT, "**", StandardOperable::arithmeticPower);

    // 3
    public static final Operator ARITHMETIC_ADD = new StandardOperator(
            7000, OperatorArity.BINARY, OperatorAssociativity.LEFT, "+", StandardOperable::arithmeticAdd);
    public static final Operator ARITHMETIC_SUBTRACT = new StandardOperator(
            7000, OperatorArity.BINARY, OperatorAssociativity.LEFT, "-", StandardOperable::arithmeticSubtract);

    // 4
    public static final Operator BITWISE_SHIFT_LEFT = new StandardOperator(
            6000, OperatorArity.BINARY, OperatorAssociativity.LEFT, "<<", StandardOperable::bitwiseShiftLeft);
    public static final Operator BITWISE_SHIFT_RIGHT = new StandardOperator(
            6000, OperatorArity.BINARY, OperatorAssociativity.LEFT, ">>", StandardOperable::bitwiseShiftRight);
    public static final Operator BITWISE_UNSIGNED_SHIFT_LEFT = new StandardOperator(
            6000, OperatorArity.BINARY, OperatorAssociativity.LEFT, "<<<", StandardOperable::bitwiseUnsignedShiftLeft);
    public static final Operator BITWISE_UNSIGNED_SHIFT_RIGHT = new StandardOperator(
            6000, OperatorArity.BINARY, OperatorAssociativity.LEFT, ">>>", StandardOperable::bitwiseUnsignedShiftRight);

    // 5
    public static final Operator RELATIONAL_GREATER_THAN = new StandardOperator(
            5000, OperatorArity.BINARY, OperatorAssociativity.LEFT, ">", StandardOperable::relationalGreaterThen);
    public static final Operator RELATIONAL_LESS_THAN = new StandardOperator(
            5000, OperatorArity.BINARY, OperatorAssociativity.LEFT, "<", StandardOperable::relationalLessThen);
    public static final Operator RELATIONAL_GREATER_THAN_OR_EQUALS = new StandardOperator(
            5000,
            OperatorArity.BINARY,
            OperatorAssociativity.LEFT,
            ">=",
            StandardOperable::relationalGreaterThenOrEquals);
    public static final Operator RELATIONAL_LESS_THAN_OR_EQUALS = new StandardOperator(
            5000, OperatorArity.BINARY, OperatorAssociativity.LEFT, "<=", StandardOperable::relationalLessThenOrEquals);

    // 6
    public static final Operator RELATIONAL_EQUALS = new StandardOperator(
            4000, OperatorArity.BINARY, OperatorAssociativity.LEFT, "==", StandardOperable::relationalEquals);
    public static final Operator RELATIONAL_NOT_EQUALS = new StandardOperator(
            4000, OperatorArity.BINARY, OperatorAssociativity.LEFT, "!=", StandardOperable::relationalNotEquals);

    // 7
    public static final Operator BITWISE_AND = new StandardOperator(
            3000, OperatorArity.BINARY, OperatorAssociativity.LEFT, "&", StandardOperable::bitwiseAnd);

    // 8
    public static final Operator BITWISE_XOR = new StandardOperator(
            2000, OperatorArity.BINARY, OperatorAssociativity.LEFT, "^", StandardOperable::bitwiseXor);

    // 9
    public static final Operator BITWISE_OR = new StandardOperator(
            1000, OperatorArity.BINARY, OperatorAssociativity.LEFT, "|", StandardOperable::bitwiseOr);

    public static final List<Operator> ALL = Collections.unmodifiableList(Arrays.asList(
            LOGICAL_NOT,
            BITWISE_NOT,
            ARITHMETIC_POSITIVE_SIGN,
            ARITHMETIC_MINUS_SIGN,
            ARITHMETIC_MULTIPLY,
            ARITHMETIC_DIVIDE,
            ARITHMETIC_MOD,
            ARITHMETIC_POWER,
            ARITHMETIC_ADD,
            ARITHMETIC_SUBTRACT,
            BITWISE_SHIFT_LEFT,
            BITWISE_SHIFT_RIGHT,
            BITWISE_UNSIGNED_SHIFT_LEFT,
            BITWISE_UNSIGNED_SHIFT_RIGHT,
            RELATIONAL_GREATER_THAN,
            RELATIONAL_LESS_THAN,
            RELATIONAL_GREATER_THAN_OR_EQUALS,
            RELATIONAL_LESS_THAN_OR_EQUALS,
            RELATIONAL_EQUALS,
            RELATIONAL_NOT_EQUALS,
            BITWISE_AND,
            BITWISE_XOR,
            BITWISE_OR));
    private static final long serialVersionUID = 3593451474476206988L;

    private final int precedence;

    @NotNull
    private final OperatorArity arity;

    @NotNull
    private final OperatorAssociativity associativity;

    @NotNull
    private final String literal;

    @NotNull
    @ToString.Exclude
    private final Operable operable;

    @NotNull
    @Override
    public Number operate(@NotNull Number... operands) {
        int arity = getArity().getArity();
        if (operands.length != arity) {
            throw new ExpressionOperationException(String.format(
                    "required %s operands to operation, but only %s were provided.", arity, operands.length));
        }
        for (int i = 0; i < operands.length; i++) {
            Validation.notNull(operands[i], String.format("the %sth operand must not be null.", i + 1));
        }
        return operable.operate(operands);
    }
}
