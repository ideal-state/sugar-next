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

package team.idealstate.minecraft.next.common.command.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import team.idealstate.minecraft.next.common.command.CommandLine;
import team.idealstate.minecraft.next.common.command.example.ExampleCommand;

/**
 * 用于标注命令行处理器
 *
 * @see ExampleCommand
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CommandHandler {

    /**
     * 命令行的范式示例，如： <br>
     * 无参：<code>reload</code> <br>
     * 有参：<code>say {message}</code> <br>
     * 命名规则为：<code>(a-zA-Z)[a-zA-Z0-9]*</code> <br>
     * 参数排列规范（规则）：<code>[字面量参数] (必选参数)</code> <br>
     * 字面量参数必须在命令行的最前端部分 <br>
     * 必选参数使用 <code>{...}</code> 包裹
     *
     * @see CommandLine#validateName(String)
     * @return 命令行的范式，为 "" 时使用方法名
     */
    String value() default "";

    /**
     * 命名规则为：<code>(a-zA-Z)[a-zA-Z0-9]*</code>
     *
     * @see CommandLine#validateName(String)
     * @return 命令行的权限节点，为 {} 时使用 {@link #value()} 中的所有参数名称的顺序拼接（可能存在分隔符，通常分隔符为 '.'）
     */
    String[] permission() default {};

    /**
     * @return 命令行是否为开放的，为 false 时将在命令行执行前进行权限节点的检查
     */
    boolean open() default false;
}
