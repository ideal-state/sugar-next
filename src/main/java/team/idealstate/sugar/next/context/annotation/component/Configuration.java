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

package team.idealstate.sugar.next.context.annotation.component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import team.idealstate.sugar.next.context.Context;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Configuration {

    /**
     * @return 配置文件的 {@link java.net.URI} 位置
     * @see Context#getResource(String)
     */
    String uri();

    /**
     * @return 配置文件的默认 {@link java.net.URI} 位置， 当 {@link #uri()} 不存在时释放（如果可以）默认配置文件到 {@link #uri()} ， 为 "" 时表示不提供默认配置文件
     * @see Context#getResource(String, Class, ClassLoader)
     */
    String release() default "";
}
