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

package team.idealstate.sugar.next.context.annotation.feature;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import team.idealstate.sugar.next.context.ContextHolder;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(RegisterParents.class)
public @interface RegisterParent {

    /** @return 父上下文的持有者类，要求当前上下文必须持有该父项时填入，此项优先级高于 {@link #optional()} */
    Class<? extends ContextHolder> required() default ContextHolder.class;

    /** @return 父上下文的持有者类的完全限定名称 {@link Class#getName()}，要求当前上下文（可选）持有该父项时填入，此项优先级低于 {@link #required()} */
    String optional() default "";
}
