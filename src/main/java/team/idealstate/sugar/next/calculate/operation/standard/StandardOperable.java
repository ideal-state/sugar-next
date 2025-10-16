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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import team.idealstate.sugar.next.calculate.exception.ExpressionOperationException;
import team.idealstate.sugar.string.StringUtils;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;
import team.idealstate.sugar.validate.annotation.Nullable;

interface StandardOperable {

    RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_UP;
    Set<Class<?>> STD_INTEGER_CLASSES = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList(Byte.class, Short.class, Integer.class, Long.class)));
    Set<Class<?>> STD_DECIMAL_CLASSES =
            Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(Float.class, Double.class)));

    static int asInt(boolean value) {
        return value ? 1 : 0;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    static boolean isStandardInteger(@NotNull Number operand) {
        if (operand instanceof BigInteger) {
            try {
                ((BigInteger) operand).longValueExact();
                return true;
            } catch (ArithmeticException e) {
                return false;
            }
        }
        return STD_INTEGER_CLASSES.contains(operand.getClass());
    }

    static boolean isStandardDecimal(@NotNull Number operand) {
        if (operand instanceof BigDecimal) {
            return ((BigDecimal) operand).compareTo(new BigDecimal(operand.doubleValue())) == 0;
        }
        return STD_DECIMAL_CLASSES.contains(operand.getClass());
    }

    @Nullable
    static BigInteger asBigInteger(@NotNull Number operand) {
        Validation.notNull(operand, "operand must not be null.");
        if (operand instanceof BigInteger) {
            return (BigInteger) operand;
        }
        if (StringUtils.isInteger(operand.toString())) {
            return new BigInteger(operand.toString());
        }
        return null;
    }

    @NotNull
    static BigDecimal asBigDecimal(@NotNull Number operand) {
        Validation.notNull(operand, "operand must not be null.");
        if (operand instanceof BigDecimal) {
            return (BigDecimal) operand;
        }
        return new BigDecimal(operand.toString());
    }

    @NotNull
    static Number logicalNot(@NotNull Number... operands) {
        Number first = operands[0];
        if (isStandardInteger(first)) {
            return first.longValue() > 0 ? 0 : 1;
        }
        return first.doubleValue() > 0 ? 0 : 1;
    }

    @NotNull
    static Number bitwiseNot(@NotNull Number... operands) {
        Number first = operands[0];
        if (isStandardInteger(first)) {
            return ~first.longValue();
        }
        BigInteger firstInt = asBigInteger(first);
        if (firstInt == null) {
            throw new ExpressionOperationException("bitwise not operation is supported only with integer.");
        }
        return firstInt.not();
    }

    @NotNull
    static Number arithmeticPositiveSign(@NotNull Number... operands) {
        return operands[0];
    }

    @NotNull
    static Number arithmeticMinusSign(@NotNull Number... operands) {
        Number first = operands[0];
        if (isStandardInteger(first)) {
            return -first.longValue();
        } else if (isStandardDecimal(first)) {
            return -first.doubleValue();
        }
        BigInteger firstInt = asBigInteger(first);
        if (firstInt != null) {
            return firstInt.negate();
        }
        return asBigDecimal(first).negate();
    }

    @NotNull
    static Number arithmeticMultiply(@NotNull Number... operands) {
        Number first = operands[0];
        Number second = operands[1];
        if (isStandardInteger(first) && isStandardInteger(second)) {
            return first.longValue() * second.longValue();
        } else if (isStandardDecimal(first) || isStandardDecimal(second)) {
            return first.doubleValue() * second.doubleValue();
        }
        BigInteger firstInt = asBigInteger(first);
        BigInteger secondInt = asBigInteger(second);
        if (firstInt != null && secondInt != null) {
            return firstInt.multiply(secondInt);
        }
        return asBigDecimal(first).multiply(asBigDecimal(second));
    }

    @NotNull
    static Number arithmeticDivide(@NotNull Number... operands) {
        Number first = operands[0];
        Number second = operands[1];
        if (isStandardInteger(first) && isStandardInteger(second)) {
            return first.longValue() / second.longValue();
        } else if (isStandardDecimal(first) || isStandardDecimal(second)) {
            return first.doubleValue() / second.doubleValue();
        }
        BigInteger firstInt = asBigInteger(first);
        BigInteger secondInt = asBigInteger(second);
        if (firstInt != null && secondInt != null) {
            return firstInt.divide(secondInt);
        }
        return asBigDecimal(first).divide(asBigDecimal(second), DEFAULT_ROUNDING_MODE);
    }

    @NotNull
    static Number arithmeticMod(@NotNull Number... operands) {
        Number first = operands[0];
        Number second = operands[1];
        if (isStandardInteger(first) && isStandardInteger(second)) {
            return first.longValue() % second.longValue();
        } else if (isStandardDecimal(first) || isStandardDecimal(second)) {
            return first.doubleValue() % second.doubleValue();
        }
        BigInteger firstInt = asBigInteger(first);
        BigInteger secondInt = asBigInteger(second);
        if (firstInt != null && secondInt != null) {
            return firstInt.mod(secondInt);
        }
        return asBigDecimal(first).remainder(asBigDecimal(second));
    }

    @NotNull
    static Number arithmeticPower(@NotNull Number... operands) {
        Number first = operands[0];
        Number second = operands[1];
        if (isStandardInteger(first) && isStandardInteger(second)) {
            return Math.pow(first.longValue(), second.longValue());
        } else if (isStandardDecimal(first) || isStandardDecimal(second)) {
            return Math.pow(first.doubleValue(), second.doubleValue());
        }
        BigInteger firstInt = asBigInteger(first);
        BigInteger secondInt = Validation.requireNotNull(asBigInteger(second), "second operand must be an integer.");
        int exponent = secondInt.intValueExact();
        if (firstInt != null) {
            if (exponent < 0) {
                exponent = -exponent;
                BigInteger positivePow = firstInt.pow(exponent);
                return BigInteger.ONE.divide(positivePow);
            }
            return firstInt.pow(exponent);
        }
        BigDecimal firstDec = asBigDecimal(first);
        if (exponent < 0) {
            exponent = -exponent;
            BigDecimal positivePow = firstDec.pow(exponent);
            return BigDecimal.ONE.divide(positivePow, DEFAULT_ROUNDING_MODE);
        }
        return firstDec.pow(exponent);
    }

    @NotNull
    static Number arithmeticAdd(@NotNull Number... operands) {
        Number first = operands[0];
        Number second = operands[1];
        if (isStandardInteger(first) && isStandardInteger(second)) {
            return first.longValue() + second.longValue();
        } else if (isStandardDecimal(first) || isStandardDecimal(second)) {
            return first.doubleValue() + second.doubleValue();
        }
        BigInteger firstInt = asBigInteger(first);
        BigInteger secondInt = asBigInteger(second);
        if (firstInt != null && secondInt != null) {
            return firstInt.add(secondInt);
        }
        return asBigDecimal(first).add(asBigDecimal(second));
    }

    @NotNull
    static Number arithmeticSubtract(@NotNull Number... operands) {
        Number first = operands[0];
        Number second = operands[1];
        if (isStandardInteger(first) && isStandardInteger(second)) {
            return first.longValue() - second.longValue();
        } else if (isStandardDecimal(first) || isStandardDecimal(second)) {
            return first.doubleValue() - second.doubleValue();
        }
        BigInteger firstInt = asBigInteger(first);
        BigInteger secondInt = asBigInteger(second);
        if (firstInt != null && secondInt != null) {
            return firstInt.subtract(secondInt);
        }
        return asBigDecimal(first).subtract(asBigDecimal(second));
    }

    @NotNull
    static Number bitwiseShiftLeft(@NotNull Number... operands) {
        Number first = operands[0];
        Number second = operands[1];
        if (isStandardInteger(first) && isStandardInteger(second)) {
            return first.longValue() << second.longValue();
        }
        BigInteger firstInt = asBigInteger(first);
        if (firstInt == null) {
            throw new ExpressionOperationException("bitwise shift left operation is supported only with integers.");
        }
        BigInteger secondInt = asBigInteger(second);
        if (secondInt == null) {
            throw new ExpressionOperationException("bitwise shift left operation is supported only with integers.");
        }
        return firstInt.shiftLeft(secondInt.intValueExact());
    }

    @NotNull
    static Number bitwiseShiftRight(@NotNull Number... operands) {
        Number first = operands[0];
        Number second = operands[1];
        if (isStandardInteger(first) && isStandardInteger(second)) {
            return first.longValue() >> second.longValue();
        }
        BigInteger firstInt = asBigInteger(first);
        if (firstInt == null) {
            throw new ExpressionOperationException("bitwise shift right operation is supported only with integers.");
        }
        BigInteger secondInt = asBigInteger(second);
        if (secondInt == null) {
            throw new ExpressionOperationException("bitwise shift right operation is supported only with integers.");
        }
        return firstInt.shiftRight(secondInt.intValueExact());
    }

    @NotNull
    static Number bitwiseUnsignedShiftLeft(@NotNull Number... operands) {
        return bitwiseShiftLeft(operands);
    }

    @NotNull
    static Number bitwiseUnsignedShiftRight(@NotNull Number... operands) {
        Number first = operands[0];
        Number second = operands[1];
        if (isStandardInteger(first) && isStandardInteger(second)) {
            return first.longValue() >>> second.longValue();
        }
        throw new ExpressionOperationException(
                "bitwise unsigned shift right operation is supported only with standard integers.");
    }

    @SuppressWarnings("DuplicatedCode")
    @NotNull
    static Number relationalGreaterThen(@NotNull Number... operands) {
        Number first = operands[0];
        Number second = operands[1];
        if (isStandardInteger(first) && isStandardInteger(second)) {
            return asInt(first.longValue() > second.longValue());
        } else if (isStandardDecimal(first) && isStandardDecimal(second)) {
            return asInt(first.doubleValue() > second.doubleValue());
        }
        BigInteger firstInt = asBigInteger(first);
        BigInteger secondInt = asBigInteger(second);
        if (firstInt != null && secondInt != null) {
            return asInt(firstInt.compareTo(secondInt) > 0);
        }
        return asInt(asBigDecimal(first).compareTo(asBigDecimal(second)) > 0);
    }

    @SuppressWarnings("DuplicatedCode")
    @NotNull
    static Number relationalLessThen(@NotNull Number... operands) {
        Number first = operands[0];
        Number second = operands[1];
        if (isStandardInteger(first) && isStandardInteger(second)) {
            return asInt(first.longValue() < second.longValue());
        } else if (isStandardDecimal(first) && isStandardDecimal(second)) {
            return asInt(first.doubleValue() < second.doubleValue());
        }
        BigInteger firstInt = asBigInteger(first);
        BigInteger secondInt = asBigInteger(second);
        if (firstInt != null && secondInt != null) {
            return asInt(firstInt.compareTo(secondInt) < 0);
        }
        return asInt(asBigDecimal(first).compareTo(asBigDecimal(second)) < 0);
    }

    @NotNull
    static Number relationalGreaterThenOrEquals(@NotNull Number... operands) {
        return (int) relationalLessThen(operands) ^ 1;
    }

    @NotNull
    static Number relationalLessThenOrEquals(@NotNull Number... operands) {
        return (int) relationalGreaterThen(operands) ^ 1;
    }

    @NotNull
    static Number relationalEquals(@NotNull Number... operands) {
        Number first = operands[0];
        Number second = operands[1];
        if (isStandardInteger(first) && isStandardInteger(second)) {
            return asInt(first.longValue() == second.longValue());
        } else if (isStandardDecimal(first) && isStandardDecimal(second)) {
            return asInt(first.doubleValue() == second.doubleValue());
        }
        BigInteger firstInt = asBigInteger(first);
        BigInteger secondInt = asBigInteger(second);
        if (firstInt != null && secondInt != null) {
            return asInt(firstInt.compareTo(secondInt) == 0);
        }
        return asInt(asBigDecimal(first).compareTo(asBigDecimal(second)) == 0);
    }

    @NotNull
    static Number relationalNotEquals(@NotNull Number... operands) {
        return (int) relationalEquals(operands) ^ 1;
    }

    @NotNull
    static Number bitwiseAnd(@NotNull Number... operands) {
        Number first = operands[0];
        Number second = operands[1];
        if (isStandardInteger(first) && isStandardInteger(second)) {
            return first.longValue() & second.longValue();
        }
        BigInteger firstInt = asBigInteger(first);
        if (firstInt == null) {
            throw new ExpressionOperationException("bitwise and operation is supported only with integers.");
        }
        BigInteger secondInt = asBigInteger(second);
        if (secondInt == null) {
            throw new ExpressionOperationException("bitwise and operation is supported only with integers.");
        }
        return firstInt.and(secondInt);
    }

    @NotNull
    static Number bitwiseXor(@NotNull Number... operands) {
        Number first = operands[0];
        Number second = operands[1];
        if (isStandardInteger(first) && isStandardInteger(second)) {
            return first.longValue() ^ second.longValue();
        }
        BigInteger firstInt = asBigInteger(first);
        if (firstInt == null) {
            throw new ExpressionOperationException("bitwise xor operation is supported only with integers.");
        }
        BigInteger secondInt = asBigInteger(second);
        if (secondInt == null) {
            throw new ExpressionOperationException("bitwise xor operation is supported only with integers.");
        }
        return firstInt.xor(secondInt);
    }

    @NotNull
    static Number bitwiseOr(@NotNull Number... operands) {
        Number first = operands[0];
        Number second = operands[1];
        if (isStandardInteger(first) && isStandardInteger(second)) {
            return first.longValue() | second.longValue();
        }
        BigInteger firstInt = asBigInteger(first);
        if (firstInt == null) {
            throw new ExpressionOperationException("bitwise or operation is supported only with integers.");
        }
        BigInteger secondInt = asBigInteger(second);
        if (secondInt == null) {
            throw new ExpressionOperationException("bitwise or operation is supported only with integers.");
        }
        return firstInt.or(secondInt);
    }
}
