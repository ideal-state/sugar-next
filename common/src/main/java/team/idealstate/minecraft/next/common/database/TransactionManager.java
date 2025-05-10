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

import team.idealstate.minecraft.next.common.database.exception.TransactionException;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

/** 此接口应交给自动化框架使用，而不是开发者手动使用 */
public interface TransactionManager {

    /**
     * @see #openTransaction(int, int)
     */
    @NotNull default TransactionSession openTransaction() {
        return openTransaction(
                DatabaseSessionFactory.DEFAULT_EXECUTION_MODE,
                DatabaseSessionFactory.DEFAULT_ISOLATION_LEVEL);
    }

    /**
     * @return 返回当前线程绑定的事务会话（不应应用新的会话参数），如果没有事务则使用参数开启一个事务会话
     * @see DatabaseSessionFactory
     */
    @NotNull TransactionSession openTransaction(int executionMode, int isolationLevel);

    /**
     * @return 从当前线程绑定的事务会话获取存储库实例
     * @throws TransactionException 如果当前线程尚未开启事务会话则抛出
     */
    @NotNull <T> T getRepository(@NotNull Class<T> repositoryType) throws TransactionException;
}
