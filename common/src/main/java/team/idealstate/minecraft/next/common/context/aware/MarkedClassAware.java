package team.idealstate.minecraft.next.common.context.aware;

import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

public interface MarkedClassAware extends Aware{

    void setMarkedClass(@NotNull Class<?> markedClass);
}
