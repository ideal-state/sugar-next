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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;

@Getter
public enum Operator implements Symbol {
    ADD("+", 1),
    SUBTRACT("-", 1),
    MULTIPLY("*", 2),
    DIVIDE("/", 2),
    MOD("%", 2),
    POWER("^", 2),
    EQUALS("==", 0),
    LESS_THAN("<", 0),
    GREATER_THAN(">", 0),
    LESS_THAN_OR_EQUALS("<=", 0),
    GREATER_THAN_OR_EQUALS(">=", 0);

    public static final Set<Character> KEYWORDS = Collections.unmodifiableSet(Arrays.stream(Operator.values())
            .map(Symbol::getSymbol)
            .map(String::toCharArray)
            .map(chars -> {
                List<Character> characters = new ArrayList<>(chars.length);
                for (char c : chars) {
                    characters.add(c);
                }
                return characters;
            })
            .flatMap(List::stream)
            .collect(HashSet::new, HashSet::add, HashSet::addAll));
    public static final int MIN_PRIORITY = Integer.MIN_VALUE;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final Set<Class<?>> INTEGER_CLASSES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(Byte.class, Short.class, Integer.class, Long.class)));
    private static final Set<Class<?>> DECIMAL_CLASSES =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(Float.class, Double.class)));
    private final String symbol;
    private final int priority;

    Operator(String symbol, int priority) {
        validateSymbol(symbol);
        validatePriority(priority);
        this.symbol = symbol;
        this.priority = priority;
    }

    private static void validateSymbol(String symbol) {
        if (Variable.hasNameContent(symbol)) {
            throw new IllegalArgumentException(
                    "Invalid symbol '" + symbol + "', because it contains a variable keyword.");
        }
        for (int i = 0; i < symbol.length(); i++) {
            char c = symbol.charAt(i);
            if (Parentheses.KEYWORDS.contains(c)) {
                throw new IllegalArgumentException(
                        "Invalid symbol '" + symbol + "', because it contains a parentheses keyword.");
            }
        }
    }

    private static void validatePriority(int priority) {
        if (priority == MIN_PRIORITY) {
            throw new IllegalArgumentException("Priority must be greater than " + MIN_PRIORITY);
        }
    }

    @NotNull
    public static BigInteger asBigInteger(@NotNull Number number) {
        Validation.notNull(number, "number must not be null.");
        if (number instanceof BigInteger) {
            return (BigInteger) number;
        }
        Class<? extends Number> numberType = number.getClass();
        if (INTEGER_CLASSES.contains(numberType)) {
            return BigInteger.valueOf(number.longValue());
        }
        return new BigInteger(number.toString());
    }

    @NotNull
    public static BigDecimal asBigDecimal(@NotNull Number number) {
        Validation.notNull(number, "number must not be null.");
        if (number instanceof BigDecimal) {
            return (BigDecimal) number;
        }
        Class<? extends Number> numberClass = number.getClass();
        if (DECIMAL_CLASSES.contains(numberClass)) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(number.toString());
    }

    public Number calculate(Number first, Number second) {
        BigDecimal firstVal = asBigDecimal(first);
        BigDecimal secondVal = asBigDecimal(second);
        switch (this) {
            case ADD:
                return firstVal.add(secondVal);
            case SUBTRACT:
                return firstVal.subtract(secondVal);
            case MULTIPLY:
                return firstVal.multiply(secondVal);
            case DIVIDE:
                return firstVal.divide(secondVal, ROUNDING_MODE);
            case MOD:
                return firstVal.remainder(secondVal);
            case POWER:
                int exponent = secondVal.intValue();
                if (exponent < 0) {
                    exponent = -exponent;
                    BigDecimal positivePow = firstVal.pow(exponent);
                    return BigDecimal.ONE.divide(positivePow, ROUNDING_MODE);
                }
                return firstVal.pow(exponent);
            case EQUALS:
                return firstVal.compareTo(secondVal) == 0 ? BigDecimal.ONE : BigDecimal.ZERO;
            case LESS_THAN:
                return firstVal.compareTo(secondVal) < 0 ? BigDecimal.ONE : BigDecimal.ZERO;
            case GREATER_THAN:
                return firstVal.compareTo(secondVal) > 0 ? BigDecimal.ONE : BigDecimal.ZERO;
            case LESS_THAN_OR_EQUALS:
                return firstVal.compareTo(secondVal) <= 0 ? BigDecimal.ONE : BigDecimal.ZERO;
            case GREATER_THAN_OR_EQUALS:
                return firstVal.compareTo(secondVal) >= 0 ? BigDecimal.ONE : BigDecimal.ZERO;
            default:
                throw new IllegalArgumentException("Unknown operator: " + this);
        }
    }
}
