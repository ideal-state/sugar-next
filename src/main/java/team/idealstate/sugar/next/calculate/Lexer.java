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
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;
import team.idealstate.sugar.next.calculate.exception.ExpressionException;
import team.idealstate.sugar.next.calculate.exception.ExpressionSyntaxException;
import team.idealstate.sugar.next.calculate.operation.Operator;
import team.idealstate.sugar.next.calculate.operation.OperatorArity;
import team.idealstate.sugar.next.calculate.operation.OperatorAssociativity;
import team.idealstate.sugar.validate.annotation.NotNull;
import team.idealstate.sugar.validate.annotation.Nullable;

final class Lexer {

    private final String expression;
    private final Map<Integer, Map<String, Set<Operator>>> operators;
    private final List<Token> tokens;
    private Character currentChar;
    private int position;
    private int line;
    private int column;
    private volatile boolean finished = false;
    private volatile ExpressionException finishedBy = null;

    public Lexer(@NotNull String expression, @NotNull Operator... operators) {
        this.expression = expression;
        Comparator<Operator> comparator =
                Comparator.comparingInt(o -> o.getLiteral().length());
        this.operators = Arrays.stream(operators)
                .sorted(comparator.reversed())
                .collect(Collectors.toMap(
                        op -> op.getLiteral().length(),
                        op -> {
                            Map<String, Set<Operator>> map = new LinkedHashMap<>();
                            map.put(op.getLiteral(), new LinkedHashSet<>(Collections.singletonList(op)));
                            return map;
                        },
                        (m1, m2) -> {
                            m2.forEach((k, v) -> m1.merge(k, v, (v1, v2) -> {
                                v1.addAll(v2);
                                return v1;
                            }));
                            return m1;
                        },
                        LinkedHashMap::new));
        this.position = 0;
        this.line = 0;
        this.column = 0;
        this.currentChar = position < expression.length() ? expression.charAt(position) : null;
        this.tokens = new ArrayList<>(Math.min(128, expression.length()));
    }

    @SuppressWarnings("SameParameterValue")
    @Nullable
    private Character peek(int offset) {
        int i = position + offset;
        return i < expression.length() ? expression.charAt(i) : null;
    }

    private void next() {
        column++;
        this.currentChar = ++position < expression.length() ? expression.charAt(position) : null;
    }

    private void skipBlank() {
        while (currentChar != null && Character.isWhitespace(currentChar)) {
            if (currentChar == '\n') {
                line++;
                this.column = 0;
            }
            next();
        }
    }

    private boolean isIdentifierStart(Character c) {
        return c != null && Character.isJavaIdentifierStart(c);
    }

    private boolean isIdentifierPart(Character c) {
        return c != null && Character.isJavaIdentifierPart(c);
    }

    @Nullable
    private Token nextToken() {
        skipBlank();

        if (currentChar == null) {
            return null;
        }

        if (currentChar == '(') {
            next();
            return createToken(TokenType.L_PAREN, "(", "(");
        }

        if (currentChar == ')') {
            next();
            return createToken(TokenType.R_PAREN, ")", ")");
        }

        Token operatorToken = readOperator();
        if (operatorToken != null) {
            return operatorToken;
        }

        if (Character.isDigit(currentChar)) {
            return readNumber();
        }

        if (isIdentifierStart(currentChar)) {
            return readIdentifier();
        }
        throw new ExpressionSyntaxException(expression, position, line, column, "Unexpected character.");
    }

    @NotNull
    private Token readNumber() {
        StringBuilder builder = new StringBuilder();
        boolean hasDecimal = false;

        while (currentChar != null) {
            if (Character.isDigit(currentChar)) {
                builder.append((char) currentChar);
                next();
            } else if (currentChar == '.' && !hasDecimal) {
                Character c = peek(1);
                if (c != null && Character.isDigit(c)) {
                    builder.append((char) currentChar);
                    next();
                    hasDecimal = true;
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        String literal = builder.toString();
        Number value;
        if (hasDecimal) {
            try {
                value = Double.parseDouble(literal);
            } catch (NumberFormatException e) {
                value = new BigDecimal(literal);
            }
        } else {
            try {
                value = Long.parseLong(literal);
            } catch (NumberFormatException e) {
                value = new BigInteger(literal);
            }
        }
        return createToken(TokenType.NUMBER, literal, value);
    }

    @NotNull
    private Token readIdentifier() {
        StringBuilder builder = new StringBuilder();

        while (isIdentifierPart(currentChar)) {
            builder.append((char) currentChar);
            next();
        }

        String literal = builder.toString();
        return createToken(TokenType.IDENTIFIER, literal, literal);
    }

    @Nullable
    private Token readOperator() {
        for (Map.Entry<Integer, Map<String, Set<Operator>>> entry : operators.entrySet()) {
            int key = entry.getKey();
            int offset = position + key;
            if (offset >= expression.length()) {
                continue;
            }
            String maybe = expression.substring(position, offset);
            Map<String, Set<Operator>> value = entry.getValue();
            if (value.containsKey(maybe)) {
                for (int i = 0; i < key; i++) {
                    next();
                }
                return createToken(TokenType.OPERATOR, maybe, new LinkedHashSet<>(value.get(maybe)));
            }
        }
        return null;
    }

    @NotNull
    private Token createToken(@NotNull TokenType type, @NotNull String literal, @NotNull Object value) {
        int literalLength = literal.length();
        return new LexerToken(type, position - literalLength, line, column - literalLength, literal, value);
    }

    @SuppressWarnings({"unchecked", "CommentedOutCode"})
    private synchronized void doTokenize() {
        if (finished) {
            if (finishedBy != null) {
                throw finishedBy;
            }
            return;
        }
        try {
            {
                Token token;
                while ((token = nextToken()) != null) {
                    this.tokens.add(token);
                }
            }
            final int totalSize = tokens.size();
            // filter
            for (int i = 0; i < totalSize; i++) {
                LexerToken token = (LexerToken) tokens.get(i);
                if (!TokenType.OPERATOR.equals(token.getType())) {
                    continue;
                }
                Set<Operator> operators = (Set<Operator>) token.getValue();
                Operator current = null;
                FILTER:
                for (Operator operator : operators) {
                    OperatorAssociativity operatorAssociativity = operator.getAssociativity();
                    if (!OperatorAssociativity.LEFT.equals(operatorAssociativity)
                            && !OperatorAssociativity.RIGHT.equals(operatorAssociativity)) {
                        throw new ExpressionSyntaxException(
                                expression,
                                token.getPosition(),
                                token.getLine(),
                                token.getColumn(),
                                "Unsupported operator.");
                    }
                    OperatorArity operatorArity = operator.getArity();
                    final int arity = operatorArity.getArity();
                    int left;
                    Set<TokenType> leftRequiredTypes;
                    int right;
                    Set<TokenType> rightRequiredTypes;

                    switch (operatorArity) {
                        case UNARY:
                            // arity always 1
                            left = right = arity;
                            if (OperatorAssociativity.LEFT.equals(operatorAssociativity)) {
                                leftRequiredTypes = new LinkedHashSet<>();
                                leftRequiredTypes.add(TokenType.NUMBER);
                                leftRequiredTypes.add(TokenType.IDENTIFIER);
                                leftRequiredTypes.add(TokenType.OPERATOR);
                                rightRequiredTypes = new LinkedHashSet<>();
                                // TAIL only supported for unary operator
                                rightRequiredTypes.add(TokenType.TAIL);
                                rightRequiredTypes.add(TokenType.OPERATOR);
                                if (i > 0) {
                                    Token leftToken = tokens.get(i - 1);
                                    if (TokenType.OPERATOR.equals(leftToken.getType())) {
                                        Operator leftOperator = (Operator) leftToken.getValue();
                                        if (!OperatorArity.UNARY.equals(leftOperator.getArity())
                                                || !operatorAssociativity.equals(leftOperator.getAssociativity())) {
                                            continue;
                                        }
                                    }
                                }
                            } else {
                                rightRequiredTypes = new LinkedHashSet<>();
                                rightRequiredTypes.add(TokenType.L_PAREN);
                                rightRequiredTypes.add(TokenType.NUMBER);
                                rightRequiredTypes.add(TokenType.IDENTIFIER);
                                rightRequiredTypes.add(TokenType.OPERATOR);
                                leftRequiredTypes = new LinkedHashSet<>();
                                // HEAD only supported for unary operator
                                leftRequiredTypes.add(TokenType.HEAD);
                                leftRequiredTypes.add(TokenType.L_PAREN);
                                leftRequiredTypes.add(TokenType.OPERATOR);
                                if (i > 0) {
                                    Token leftToken = tokens.get(i - 1);
                                    if (TokenType.OPERATOR.equals(leftToken.getType())) {
                                        Operator leftOperator = (Operator) leftToken.getValue();
                                        if (OperatorArity.UNARY.equals(leftOperator.getArity())) {
                                            if (!operatorAssociativity.equals(leftOperator.getAssociativity())) {
                                                continue;
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                        case BINARY:
                            // arity always 2
                            left = right = arity / 2;
                            leftRequiredTypes = new LinkedHashSet<>();
                            leftRequiredTypes.add(TokenType.R_PAREN);
                            leftRequiredTypes.add(TokenType.NUMBER);
                            leftRequiredTypes.add(TokenType.IDENTIFIER);
                            leftRequiredTypes.add(TokenType.OPERATOR);
                            rightRequiredTypes = new LinkedHashSet<>();
                            rightRequiredTypes.add(TokenType.L_PAREN);
                            rightRequiredTypes.add(TokenType.NUMBER);
                            rightRequiredTypes.add(TokenType.IDENTIFIER);
                            rightRequiredTypes.add(TokenType.OPERATOR);
                            if (i > 0) {
                                Token leftToken = tokens.get(i - 1);
                                if (TokenType.OPERATOR.equals(leftToken.getType())) {
                                    Operator leftOperator = (Operator) leftToken.getValue();
                                    if (OperatorArity.UNARY.equals(leftOperator.getArity())) {
                                        if (OperatorAssociativity.RIGHT.equals(leftOperator.getAssociativity())) {
                                            continue;
                                        }
                                    } else {
                                        continue;
                                    }
                                }
                            }
                            break;
                        default:
                            throw new ExpressionSyntaxException(
                                    expression,
                                    token.getPosition(),
                                    token.getLine(),
                                    token.getColumn(),
                                    "Unsupported operator.");
                    }
                    //noinspection ConstantValue
                    if (left == 0 && right == 0) {
                        continue;
                    }
                    if (left > 0) {
                        for (int j = 1; j <= left; j++) {
                            int index = i - j;
                            if (index < 0) {
                                // HEAD only supported for unary operator
                                if (!leftRequiredTypes.contains(TokenType.HEAD)) {
                                    continue FILTER;
                                }
                                break;
                            }
                            if (!leftRequiredTypes.contains(tokens.get(index).getType())) {
                                continue FILTER;
                            }
                        }
                    }
                    if (right > 0) {
                        for (int j = 1; j <= right; j++) {
                            int index = i + j;
                            if (index >= totalSize) {
                                // TAIL only supported for unary operator
                                if (!rightRequiredTypes.contains(TokenType.TAIL)) {
                                    continue FILTER;
                                }
                                break;
                            }
                            if (!rightRequiredTypes.contains(tokens.get(index).getType())) {
                                continue FILTER;
                            }
                        }
                    }
                    if (current == null) {
                        current = operator;
                    } else {
                        throw new ExpressionSyntaxException(
                                expression,
                                token.getPosition(),
                                token.getLine(),
                                token.getColumn(),
                                "operators with multiple similar definitions.");
                    }
                }
                if (current == null) {
                    throw new ExpressionSyntaxException(
                            expression, token.getPosition(), token.getLine(), token.getColumn(), "Invalid operator.");
                }
                token.setValue(current);
            }
        } catch (ExpressionException e) {
            this.finishedBy = e;
            throw e;
        } catch (Throwable e) {
            this.finishedBy = new ExpressionException(e);
            throw finishedBy;
        } finally {
            this.finished = true;
        }
    }

    @NotNull
    public List<Token> tokenize() {
        if (!finished) {
            doTokenize();
        }
        if (finishedBy != null) {
            throw finishedBy;
        }
        if (tokens.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(tokens);
    }

    @NotNull
    public List<Token> rpn() {
        List<Token> tokens = tokenize();
        List<Token> rpn = new ArrayList<>(tokens.size());
        Deque<Token> stack = new ArrayDeque<>(tokens.size());
        for (Token token : tokens) {
            switch (token.getType()) {
                case L_PAREN:
                    stack.push(token);
                    break;
                case R_PAREN:
                    while (true) {
                        Token peeked = stack.peek();
                        if (stack.isEmpty()) {
                            throw new ExpressionSyntaxException(
                                    expression,
                                    token.getPosition(),
                                    token.getLine(),
                                    token.getColumn(),
                                    "Unexpected character.");
                        }
                        if (peeked.getType() == TokenType.L_PAREN) {
                            stack.pop();
                            break;
                        }
                        rpn.add(stack.pop());
                    }
                    break;
                case NUMBER:
                case IDENTIFIER:
                    rpn.add(token);
                    break;
                case OPERATOR:
                    Operator current = (Operator) token.getValue();
                    Token peeked;
                    while (!stack.isEmpty()
                            && (peeked = stack.peek()) != null
                            && peeked.getType() != TokenType.L_PAREN) {
                        Operator last = (Operator) peeked.getValue();
                        if ((OperatorAssociativity.LEFT.equals(last.getAssociativity())
                                        && current.getPrecedence() <= last.getPrecedence())
                                || (!OperatorAssociativity.LEFT.equals(last.getAssociativity())
                                        && current.getPrecedence() < last.getPrecedence())) {
                            rpn.add(stack.pop());
                        } else {
                            break;
                        }
                    }
                    stack.push(token);
                    break;
                default:
                    throw new ExpressionSyntaxException(
                            expression, token.getPosition(), token.getLine(), token.getColumn(), "Unsupported token.");
            }
        }
        while (!stack.isEmpty()) {
            rpn.add(stack.pop());
        }
        return rpn;
    }

    @Data
    @AllArgsConstructor
    private static final class LexerToken implements Token {

        @NotNull
        private final TokenType type;

        private final int position;
        private final int line;
        private final int column;

        @NotNull
        private final String literal;

        @NotNull
        @Setter(AccessLevel.PRIVATE)
        private Object value;
    }
}
