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

package team.idealstate.minecraft.next.common.logging;

public enum LogLevel {
    UNKNOWN(0, ""),
    TRACE(1, "§f"),
    DEBUG(2, "§7"),
    INFO(3, "§a"),
    WARN(4, "§e"),
    ERROR(5, "§c"),
    FATAL(6, "§4");

    public final int level;
    public final String colorCode;

    LogLevel(int level, String colorCode) {
        this.level = level;
        this.colorCode = colorCode;
    }
}
