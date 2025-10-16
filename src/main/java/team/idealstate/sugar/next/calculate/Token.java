package team.idealstate.sugar.next.calculate;

import team.idealstate.sugar.validate.annotation.NotNull;

interface Token {

    @NotNull
    TokenType getType();

    int getPosition();

    int getLine();

    int getColumn();

    @NotNull
    String getLiteral();

    @NotNull
    Object getValue();
}
