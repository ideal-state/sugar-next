package team.idealstate.sugar.next.calculate.operation;

import team.idealstate.sugar.validate.annotation.NotNull;

public interface Operator extends Operable {

    int getPrecedence();

    @NotNull
    OperatorArity getArity();

    @NotNull
    OperatorAssociativity getAssociativity();

    @NotNull
    String getLiteral();
}
