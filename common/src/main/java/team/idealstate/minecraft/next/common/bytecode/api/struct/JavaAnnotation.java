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

package team.idealstate.minecraft.next.common.bytecode.api.struct;

import static team.idealstate.minecraft.next.common.reflect.Reflection.annotation;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import team.idealstate.minecraft.next.common.bytecode.Java;
import team.idealstate.minecraft.next.common.bytecode.api.JavaAnnotatedElement;
import team.idealstate.minecraft.next.common.bytecode.api.member.JavaClass;
import team.idealstate.minecraft.next.common.bytecode.exception.BytecodeException;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;
import team.idealstate.minecraft.next.common.validate.annotation.Nullable;

public interface JavaAnnotation extends Java<Annotation> {

    @NotNull @Override
    @SuppressWarnings({"unchecked"})
    default <R extends Annotation> R java(@Nullable ClassLoader classLoader)
            throws BytecodeException {
        Map<String, Object> mappings = getMappings();
        for (Map.Entry<String, Object> entry : mappings.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Java) {
                entry.setValue(((Java<?>) value).java(classLoader));
            } else if (value.getClass().isArray()) {
                Object[] arr = (Object[]) value;
                for (int i = 0; i < arr.length; i++) {
                    if (arr[i] instanceof Java) {
                        arr[i] = ((Java<?>) arr[i]).java(classLoader);
                    }
                }
            }
        }
        return (R)
                annotation(
                        (Class<? extends Annotation>) getAnnotationType().java(classLoader),
                        mappings);
    }

    @NotNull JavaClass getAnnotationType();

    @Nullable JavaAnnotatedElement getAnnotatedElement();

    @NotNull Set<String> getMappingNames();

    @NotNull <V> V getMappingValue(@NotNull String mappingName);

    @NotNull default Map<String, Object> getMappings() {
        Set<String> mappingNames = getMappingNames();
        Map<String, Object> mappings = new HashMap<>(mappingNames.size());
        for (String mappingName : mappingNames) {
            mappings.put(mappingName, getMappingValue(mappingName));
        }
        return mappings;
    }
}
