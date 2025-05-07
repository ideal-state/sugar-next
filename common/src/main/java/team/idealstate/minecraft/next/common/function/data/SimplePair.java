package team.idealstate.minecraft.next.common.function.data;

import lombok.Data;

@Data
final class SimplePair<F, S> implements Pair<F, S> {

    private final F first;
    private final S second;
}
