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

package team.idealstate.minecraft.next.common.context.boot.mybatis.log;

import team.idealstate.minecraft.next.common.logging.Log;
import team.idealstate.minecraft.next.common.logging.LogLevel;
import team.idealstate.minecraft.next.common.stacktrace.StackTraceUtils;

public class LogImpl implements org.apache.ibatis.logging.Log {
    @Override
    public boolean isDebugEnabled() {
        return LogLevel.DEBUG.getIndex() >= Log.getLevel().getIndex();
    }

    @Override
    public boolean isTraceEnabled() {
        return LogLevel.TRACE.getIndex() >= Log.getLevel().getIndex();
    }

    @Override
    public void error(String s, Throwable e) {
        Log.error(() -> s + "\n" + StackTraceUtils.makeDetails(e));
    }

    @Override
    public void error(String s) {
        Log.error(s);
    }

    @Override
    public void debug(String s) {
        Log.debug(s);
    }

    @Override
    public void trace(String s) {
        Log.trace(s);
    }

    @Override
    public void warn(String s) {
        Log.warn(s);
    }
}
