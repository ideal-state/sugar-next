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

package team.idealstate.minecraft.next.common.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import team.idealstate.minecraft.next.common.bundled.Bundled;
import team.idealstate.minecraft.next.common.context.Context;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NextConfiguration {

    String BUNDLED = "bundled:";

    /**
     * @return 配置文件的路径， 前缀为 {@link #BUNDLED} 时将使用嵌入资源的路径匹配方式（{@link Bundled}）， 其它则使用相对路径的匹配方式（{@link
     *     Context#getDataFolder()}）
     */
    String value();
}
