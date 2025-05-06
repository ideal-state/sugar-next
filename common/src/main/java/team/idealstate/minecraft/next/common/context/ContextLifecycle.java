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

package team.idealstate.minecraft.next.common.context;

import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

public interface ContextLifecycle {

    void onInitialize(@NotNull Context context);

    void onInitialized(@NotNull Context context);

    void onLoad(@NotNull Context context);

    void onLoaded(@NotNull Context context);

    void onEnable(@NotNull Context context);

    void onEnabled(@NotNull Context context);

    void onDisable(@NotNull Context context);

    void onDisabled(@NotNull Context context);

    void onDestroy(@NotNull Context context);

    void onDestroyed(@NotNull Context context);
}
