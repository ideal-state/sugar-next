package team.idealstate.minecraft.next.common.function.data;

import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

public interface Pair<F, S> {

    @NotNull
    static <F, S> Pair<F, S> of(F first, S second) {
        return new SimplePair<>(first, second);
    }

    F getFirst();

    S getSecond();
}
