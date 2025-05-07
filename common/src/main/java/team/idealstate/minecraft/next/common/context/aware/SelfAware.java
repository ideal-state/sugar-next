package team.idealstate.minecraft.next.common.context.aware;

import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

public interface SelfAware<T> extends Aware {

    void setSelf(@NotNull T self);
}
