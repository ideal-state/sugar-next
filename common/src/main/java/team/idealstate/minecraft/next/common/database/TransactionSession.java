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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

/** 此接口应交给自动化框架使用，而不是开发者手动使用 */
@RequiredArgsConstructor
public final class TransactionSession implements DatabaseSession {
    @NonNull private final DatabaseSession databaseSession;
    @NonNull private final Runnable closer;

    @NotNull @Override
    public <T> T getRepository(@NotNull Class<T> repositoryType) {
        return databaseSession.getRepository(repositoryType);
    }

    private int count = 0;

    /**
     * @return 应返回 this，自动化框架应在每个使用事务的方法栈调用仅一次此方法， 以更新当前事务会话的引用计数， 确保事务会话在顶层方法栈调用结束后被关闭
     */
    @NotNull public TransactionSession open() {
        count++;
        return this;
    }

    @Override
    public void commit() {
        if (count <= 1) {
            databaseSession.commit();
        }
    }

    @Override
    public void rollback() {
        if (count <= 1) {
            databaseSession.rollback();
        }
    }

    @Override
    public void close() {
        count--;
        if (count <= 0) {
            try {
                closer.run();
            } finally {
                databaseSession.close();
            }
        }
    }
}
