package team.idealstate.sugar.next.calculate.operation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public enum OperatorArity {

    UNARY(1),
    BINARY(2),
    TERNARY(3);

    private final int arity;
}
