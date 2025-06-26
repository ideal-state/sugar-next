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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import team.idealstate.sugar.next.context.Context;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DependsOn {

    /** @return 依赖的 {@link Bean} */
    Bean[] value() default {};

    /**
     * @return 依赖的 {@link team.idealstate.sugar.next.context.Bean#getName()}
     * @deprecated 不含明确细节的依赖项，使用 {@link #value()} 替代
     */
    @Deprecated
    String[] beans() default {};

    /** @return 依赖的 {@link Class#getName()} */
    String[] classes() default {};

    /** @return 依赖的 {@link Context#getProperty(String)} */
    Property[] properties() default {};

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface Bean {

        /** @return 依赖的 {@link team.idealstate.sugar.next.context.Bean#getName()} */
        String value();

        /** @return 依赖的 {@link team.idealstate.sugar.next.context.Bean#getType()} */
        Class<?> type() default Object.class;

        boolean inherited() default Context.DEFAULT_OPTION_INHERITED;
    }

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface Property {

        String key();

        String value() default "";

        boolean strict() default true;
    }
}
