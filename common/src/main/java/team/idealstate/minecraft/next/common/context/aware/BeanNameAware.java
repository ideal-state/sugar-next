package team.idealstate.minecraft.next.common.context.aware;

import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

public interface BeanNameAware extends Aware {

    void setBeanName(@NotNull String beanName);
}
