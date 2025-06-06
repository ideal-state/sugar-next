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

package team.idealstate.sugar.next.function;

import team.idealstate.sugar.next.function.closure.Provider;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;

final class CachedLazy<V> implements Lazy<V> {

    private volatile Provider<V> provider;
    private volatile V value;

    public CachedLazy(V value) {
        this.value = value;
    }

    public CachedLazy(@NotNull Provider<V> provider) {
        Validation.notNull(provider, "provider must not be null.");
        this.provider = provider;
    }

    @Override
    public V get() {
        if (provider != null) {
            synchronized (this) {
                if (provider != null) {
                    value = provider.provide();
                    provider = null;
                }
            }
        }
        return value;
    }

    @Override
    public boolean isInitialized() {
        return provider == null;
    }
}
