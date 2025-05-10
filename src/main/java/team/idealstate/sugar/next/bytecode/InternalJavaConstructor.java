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

import team.idealstate.sugar.next.bytecode.api.member.JavaClass;
import team.idealstate.sugar.next.bytecode.api.member.JavaConstructor;
import team.idealstate.sugar.next.bytecode.api.member.JavaMethod;
import team.idealstate.sugar.next.bytecode.api.member.JavaParameter;
import team.idealstate.sugar.next.bytecode.api.struct.JavaAnnotation;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;

class InternalJavaConstructor implements JavaConstructor {

    private final JavaMethod delegate;

    InternalJavaConstructor(@NotNull JavaMethod delegate) {
        Validation.notNull(delegate, "delegate must not be null.");
        this.delegate = delegate;
    }

    @NotNull
    @Override
    public JavaClass getDeclaringClass() {
        return delegate.getDeclaringClass();
    }

    @NotNull
    @Override
    public String getName() {
        return delegate.getName();
    }

    @NotNull
    @Override
    public JavaParameter[] getParameters() {
        return delegate.getParameters();
    }

    @NotNull
    @Override
    public JavaClass[] getExceptionTypes() {
        return delegate.getExceptionTypes();
    }

    @Override
    public int getAccess() {
        return delegate.getAccess();
    }

    @NotNull
    @Override
    public JavaAnnotation[] getAnnotations() {
        return delegate.getAnnotations();
    }
}
