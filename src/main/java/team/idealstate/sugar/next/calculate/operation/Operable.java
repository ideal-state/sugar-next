package team.idealstate.sugar.next.calculate.operation;

import team.idealstate.sugar.validate.annotation.NotNull;

public interface Operable {

    @NotNull
    Number operate(@NotNull Number... operands);
}
