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

package team.idealstate.minecraft.next.common.function;

import team.idealstate.minecraft.next.common.function.closure.Action;
import team.idealstate.minecraft.next.common.function.closure.Condition;
import team.idealstate.minecraft.next.common.function.closure.Function;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

class ConditionFunctional<T> implements Functional<T> {

    private final T it;
    private final Condition<T> condition;

    ConditionFunctional(T it, Condition<T> condition) {
        this.it = it;
        this.condition = condition;
    }

    private boolean condition() {
        if (condition == null) {
            return true;
        }
        return condition.test(it);
    }

    @Override
    public T it() {
        return it;
    }

    @Override
    public Functional<T> apply(@NotNull Action<T> action) {
        if (condition()) {
            return Functional.super.apply(action);
        }
        return this;
    }

    @Override
    public void run(@NotNull Action<T> action) {
        if (condition()) {
            Functional.super.run(action);
        }
    }

    @Override
    public void use(@NotNull Action<T> action) {
        if (condition()) {
            Functional.super.use(action);
        }
    }

    @Override
    public <R> R use(@NotNull Class<R> returnType, @NotNull Function<T, R> function) {
        if (condition()) {
            return Functional.super.use(returnType, function);
        }
        return null;
    }

    @Override
    public Functional<T> when(Condition<T> condition) {
        if (condition()) {
            return Functional.super.when(condition);
        }
        return this;
    }
}
