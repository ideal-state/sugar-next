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

package team.idealstate.minecraft.next.common.bytecode;

import static team.idealstate.minecraft.next.common.bytecode.Java.typeof;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Type;
import team.idealstate.minecraft.next.common.bytecode.api.member.JavaClass;
import team.idealstate.minecraft.next.common.bytecode.api.member.JavaField;
import team.idealstate.minecraft.next.common.bytecode.api.struct.JavaAnnotation;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;
import team.idealstate.minecraft.next.common.validate.annotation.Nullable;

class InternalJavaField implements JavaField {

    private final JavaClass declaringClass;
    private final String name;
    private final String typeName;
    private final Object defaultValue;
    private final int access;
    private final List<JavaAnnotation> annotations = new ArrayList<>(8);
    private final JavaCache cache;

    InternalJavaField(
            @NotNull JavaClass declaringClass,
            int access,
            @NotNull String name,
            @NotNull String typeName,
            @Nullable Object defaultValue,
            @NotNull JavaCache cache) {
        Validation.notNull(declaringClass, "declaringClass must not be null.");
        Validation.notNull(name, "name must not be null.");
        Validation.notNull(typeName, "typeName must not be null.");
        Validation.notNull(cache, "cache must not be null.");
        this.declaringClass = declaringClass;
        this.access = access;
        this.name = name;
        this.typeName = typeName;
        this.defaultValue = defaultValue;
        this.cache = cache;
    }

    @NotNull @Override
    public JavaClass getDeclaringClass() {
        return declaringClass;
    }

    @NotNull @Override
    public String getName() {
        return name;
    }

    @NotNull @Override
    public JavaClass getType() {
        return typeof(typeName, cache);
    }

    @Nullable @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public int getAccess() {
        return access;
    }

    @NotNull @Override
    public JavaAnnotation[] getAnnotations() {
        return annotations.toArray(new JavaAnnotation[0]);
    }

    static class Visitor extends FieldVisitor {

        private final InternalJavaField internalJavaField;
        private final JavaCache cache;

        Visitor(
                int api,
                FieldVisitor fieldVisitor,
                @NotNull InternalJavaField internalJavaField,
                @NotNull JavaCache cache) {
            super(api, fieldVisitor);
            Validation.notNull(internalJavaField, "internalJavaField must not be null.");
            Validation.notNull(cache, "cache must not be null.");
            this.internalJavaField = internalJavaField;
            this.cache = cache;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            AnnotationVisitor annotationVisitor = super.visitAnnotation(descriptor, visible);
            InternalJavaAnnotation internalJavaAnnotation =
                    new InternalJavaAnnotation(
                            Type.getType(descriptor).getClassName(), internalJavaField, cache);
            annotationVisitor =
                    new InternalJavaAnnotation.Visitor(
                            api, annotationVisitor, internalJavaAnnotation, cache);
            internalJavaField.annotations.add(internalJavaAnnotation);
            return annotationVisitor;
        }
    }
}
