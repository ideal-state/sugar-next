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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import team.idealstate.minecraft.next.common.bytecode.api.member.JavaClass;
import team.idealstate.minecraft.next.common.bytecode.api.member.JavaMethod;
import team.idealstate.minecraft.next.common.bytecode.api.member.JavaParameter;
import team.idealstate.minecraft.next.common.bytecode.api.struct.JavaAnnotation;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;
import team.idealstate.minecraft.next.common.validate.annotation.Nullable;

class InternalJavaMethod implements JavaMethod {

    private final JavaClass declaringClass;
    private final int access;
    private final String name;
    private final int parameterCount;
    private final String[] exceptionTypeNames;
    private final List<JavaParameter> parameters = new ArrayList<>(8);
    private final String returnTypeName;
    private final List<JavaAnnotation> annotations = new ArrayList<>(8);
    private Object defaultValue;

    InternalJavaMethod(
            @NotNull JavaClass declaringClass,
            int access,
            @NotNull String name,
            int parameterCount,
            @NotNull String returnTypeName,
            String[] exceptionTypeNames) {
        Validation.notNull(declaringClass, "declaringClass must not be null.");
        Validation.notNull(name, "name must not be null.");
        Validation.notNull(returnTypeName, "returnTypeName must not be null.");
        this.declaringClass = declaringClass;
        this.access = access;
        this.name = name;
        this.parameterCount = parameterCount;
        this.returnTypeName = returnTypeName;
        this.exceptionTypeNames = exceptionTypeNames;
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
    public JavaParameter[] getParameters() {
        return parameters.toArray(new JavaParameter[0]);
    }

    @NotNull @Override
    public JavaClass getReturnType() {
        return typeof(returnTypeName);
    }

    @NotNull @Override
    public JavaClass[] getExceptionTypes() {
        return exceptionTypeNames == null
                ? new JavaClass[0]
                : Arrays.stream(exceptionTypeNames).map(Java::typeof).toArray(JavaClass[]::new);
    }

    @Nullable @Override
    public Object getDefaultValue() {
        return InternalJavaAnnotation.AbstractVisitor.unwrapValue(defaultValue);
    }

    @Override
    public int getAccess() {
        return access;
    }

    @NotNull @Override
    public JavaAnnotation[] getAnnotations() {
        return annotations.toArray(new JavaAnnotation[0]);
    }

    static class Visitor extends MethodVisitor {

        private final InternalJavaMethod internalJavaMethod;
        private final Map<Integer, InternalJavaParameter> parameterMap = new HashMap<>(8);

        Visitor(
                int api,
                MethodVisitor methodVisitor,
                @NotNull InternalJavaMethod internalJavaMethod) {
            super(api, methodVisitor);
            Validation.notNull(internalJavaMethod, "internalJavaMethod must not be null.");
            this.internalJavaMethod = internalJavaMethod;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            AnnotationVisitor annotationVisitor = super.visitAnnotation(descriptor, visible);
            InternalJavaAnnotation internalJavaAnnotation =
                    new InternalJavaAnnotation(
                            Type.getType(descriptor).getClassName(), internalJavaMethod);
            annotationVisitor =
                    new InternalJavaAnnotation.Visitor(
                            api, annotationVisitor, internalJavaAnnotation);
            internalJavaMethod.annotations.add(internalJavaAnnotation);
            return annotationVisitor;
        }

        protected final int recalculateIndex(int index) {
            if (internalJavaMethod.isStatic()) {
                return index;
            }
            return index - 1;
        }

        protected final boolean isParameterIndex(int index) {
            return index >= 0 && index < internalJavaMethod.parameterCount;
        }

        @Override
        public void visitLocalVariable(
                String name,
                String descriptor,
                String signature,
                Label start,
                Label end,
                int index) {
            super.visitLocalVariable(name, descriptor, signature, start, end, index);
            int recalculatedIndex = recalculateIndex(index);
            if (isParameterIndex(recalculatedIndex)) {
                InternalJavaParameter internalJavaParameter =
                        parameterMap.computeIfAbsent(
                                index, i -> new InternalJavaParameter(internalJavaMethod));
                internalJavaParameter.index = recalculatedIndex;
                internalJavaParameter.name = name;
                internalJavaParameter.typeName = Type.getType(descriptor).getClassName();
                internalJavaMethod.parameters.add(internalJavaParameter);
            }
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(
                int index, String descriptor, boolean visible) {
            AnnotationVisitor annotationVisitor =
                    super.visitParameterAnnotation(index, descriptor, visible);
            if (isParameterIndex(index)) {
                InternalJavaParameter internalJavaParameter =
                        parameterMap.computeIfAbsent(
                                index, i -> new InternalJavaParameter(internalJavaMethod));
                InternalJavaAnnotation internalJavaAnnotation =
                        new InternalJavaAnnotation(
                                Type.getType(descriptor).getClassName(), internalJavaParameter);
                annotationVisitor =
                        new InternalJavaAnnotation.Visitor(
                                api, annotationVisitor, internalJavaAnnotation);
                internalJavaParameter.annotations.add(internalJavaAnnotation);
            }
            return annotationVisitor;
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
        }

        @Override
        public AnnotationVisitor visitAnnotationDefault() {
            AnnotationVisitor annotationVisitor = super.visitAnnotationDefault();
            annotationVisitor =
                    new AnnotationDefaultVisitor(api, annotationVisitor, internalJavaMethod);
            return annotationVisitor;
        }

        private static class AnnotationDefaultVisitor
                extends InternalJavaAnnotation.AbstractVisitor {

            private final InternalJavaMethod internalJavaMethod;

            public AnnotationDefaultVisitor(
                    int api,
                    AnnotationVisitor annotationVisitor,
                    @NotNull InternalJavaMethod internalJavaMethod) {
                super(api, annotationVisitor);
                Validation.notNull(internalJavaMethod, "internalJavaMethod must not be null.");
                this.internalJavaMethod = internalJavaMethod;
            }

            @Override
            protected void put(String name, Object value) {
                internalJavaMethod.defaultValue = value;
            }
        }
    }

    private static class InternalJavaParameter implements JavaParameter {

        private final JavaMethod declaringMethod;
        private final List<JavaAnnotation> annotations = new ArrayList<>(8);
        private int index;
        private String name;
        private String typeName;

        public InternalJavaParameter(@NotNull JavaMethod declaringMethod) {
            Validation.notNull(declaringMethod, "declaringMethod must not be null");
            this.declaringMethod = declaringMethod;
        }

        @NotNull @Override
        public JavaMethod getDeclaringMethod() {
            return declaringMethod;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @NotNull @Override
        public String getName() {
            Validation.notNull(name, "name must not be null");
            return name;
        }

        @NotNull @Override
        public JavaClass getType() {
            Validation.notNull(typeName, "typeName must not be null");
            return typeof(typeName);
        }

        @NotNull @Override
        public JavaAnnotation[] getAnnotations() {
            return annotations.toArray(new JavaAnnotation[0]);
        }
    }
}
