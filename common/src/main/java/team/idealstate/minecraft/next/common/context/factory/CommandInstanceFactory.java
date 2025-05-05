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

package team.idealstate.minecraft.next.common.context.factory;

import java.lang.annotation.Annotation;
import team.idealstate.minecraft.next.common.command.CommandLine;
import team.idealstate.minecraft.next.common.context.Context;
import team.idealstate.minecraft.next.common.logging.Log;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

public abstract class CommandInstanceFactory<M extends Annotation>
        extends NoArgsConstructorInstanceFactory<M, Object> {
    protected CommandInstanceFactory(@NotNull Class<M> metadataClass) {
        super(metadataClass, Object.class);
    }

    @NotNull protected abstract String getCommandName(@NotNull M metadata);

    @Override
    protected boolean doCanBeCreated(
            @NotNull Context context, @NotNull M metadata, @NotNull Class<?> marked) {
        String name = getCommandName(metadata);
        try {
            CommandLine.validateName(name);
        } catch (IllegalArgumentException e) {
            Log.warn(() -> getMetadataClass().getSimpleName() + ": Invalid command name: " + name);
            return false;
        }
        return super.doCanBeCreated(context, metadata, marked);
    }
}
