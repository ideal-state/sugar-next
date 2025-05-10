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

package team.idealstate.minecraft.next.common.database;

import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

public interface DatabaseSessionFactory {

    int DEFAULT_EXECUTION_MODE = Integer.MIN_VALUE;
    int DEFAULT_ISOLATION_LEVEL = Integer.MIN_VALUE;

    /**
     * @return 数据库会话，应使用默认的会话参数
     */
    @NotNull default DatabaseSession openSession() {
        return openSession(DEFAULT_EXECUTION_MODE, DEFAULT_ISOLATION_LEVEL);
    }

    /**
     * @param executionMode 执行模式
     * @param isolationLevel 隔离级别
     * @return 数据库会话，会话参数是否生效应查看其实现类，这只是标准而不是必须的
     * @see #DEFAULT_EXECUTION_MODE 实现类的默认执行模式应该使用此值
     * @see #DEFAULT_ISOLATION_LEVEL 实现类的默认隔离级别应该使用此值
     */
    @NotNull DatabaseSession openSession(int executionMode, int isolationLevel);
}
