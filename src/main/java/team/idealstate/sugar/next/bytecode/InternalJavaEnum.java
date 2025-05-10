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

package team.idealstate.sugar.next.bytecode;

import static team.idealstate.sugar.next.bytecode.Java.typeof;

import team.idealstate.sugar.next.bytecode.api.member.JavaClass;
import team.idealstate.sugar.next.bytecode.api.struct.JavaEnum;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;

class InternalJavaEnum implements JavaEnum {

    private final String enumTypeName;
    private final String name;
    private final JavaCache cache;

    public InternalJavaEnum(@NotNull String enumTypeName, @NotNull String name, @NotNull JavaCache cache) {
        Validation.notNull(enumTypeName, "enumTypeName must not be null.");
        Validation.notNull(name, "name must not be null.");
        Validation.notNull(cache, "cache must not be null.");
        this.enumTypeName = enumTypeName;
        this.name = name;
        this.cache = cache;
    }

    @NotNull
    @Override
    public JavaClass getEnumType() {
        return typeof(enumTypeName, cache);
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }
}
