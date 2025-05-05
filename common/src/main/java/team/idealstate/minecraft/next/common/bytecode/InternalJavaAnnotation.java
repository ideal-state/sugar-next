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
import static team.idealstate.minecraft.next.common.function.Functional.lazy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;
import team.idealstate.minecraft.next.common.bytecode.api.JavaAnnotatedElement;
import team.idealstate.minecraft.next.common.bytecode.api.member.JavaClass;
import team.idealstate.minecraft.next.common.bytecode.api.struct.JavaAnnotation;
import team.idealstate.minecraft.next.common.function.Lazy;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;
import team.idealstate.minecraft.next.common.validate.annotation.Nullable;

class InternalJavaAnnotation implements JavaAnnotation {

    private final String annotationTypeName;
    private final JavaAnnotatedElement annotatedElement;
    private final Map<String, Object> mappings = new HashMap<>(16, 0.6F);
    private final JavaCache cache;

    InternalJavaAnnotation(
            @NotNull String annotationTypeName,
            @Nullable JavaAnnotatedElement annotatedElement,
            @NotNull JavaCache cache) {
        Validation.notNull(annotationTypeName, "annotationTypeName must not be null.");
        Validation.notNull(cache, "cache must not be null.");
        this.annotationTypeName = annotationTypeName;
        this.annotatedElement = annotatedElement;
        this.cache = cache;
    }

    @NotNull @Override
    public JavaClass getAnnotationType() {
        return typeof(annotationTypeName, cache);
    }

    @Nullable @Override
    public JavaAnnotatedElement getAnnotatedElement() {
        return annotatedElement;
    }

    @NotNull @Override
    public Set<String> getMappingNames() {
        return new HashSet<>(mappings.keySet());
    }

    @NotNull @Override
    public <V> V getMappingValue(@NotNull String mappingName) {
        Object value = mappings.get(mappingName);
        return AbstractVisitor.unwrapValue(value);
    }

    abstract static class AbstractVisitor extends AnnotationVisitor {

        protected final JavaCache cache;

        AbstractVisitor(int api, AnnotationVisitor annotationVisitor, @NotNull JavaCache cache) {
            super(api, annotationVisitor);
            Validation.notNull(cache, "cache must not be null.");
            this.cache = cache;
        }

        @SuppressWarnings({"unchecked"})
        public static <V> V unwrapValue(Object value) {
            if (value instanceof Lazy) {
                value = ((Lazy<?>) value).get();
            } else if (value instanceof List) {
                value = ((List<?>) value).toArray();
                Object[] arr = (Object[]) value;
                for (int i = 0; i < arr.length; i++) {
                    if (arr[i] instanceof Lazy) {
                        arr[i] = ((Lazy<?>) arr[i]).get();
                    }
                }
            }
            return (V) value;
        }

        protected abstract void put(String name, Object value);

        @Override
        public void visit(String name, Object value) {
            super.visit(name, value);
            if (value instanceof Type) {
                Lazy<JavaClass> lazy = lazy(() -> typeof(((Type) value).getClassName(), cache));
                put(name, lazy);
            } else {
                put(name, value);
            }
        }

        @Override
        public void visitEnum(String name, String descriptor, String value) {
            super.visitEnum(name, descriptor, value);
            put(name, new InternalJavaEnum(Type.getType(descriptor).getClassName(), value, cache));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
            AnnotationVisitor annotationVisitor = super.visitAnnotation(name, descriptor);
            InternalJavaAnnotation internalJavaAnnotation =
                    new InternalJavaAnnotation(
                            Type.getType(descriptor).getClassName(), null, cache);
            annotationVisitor = new Visitor(api, annotationVisitor, internalJavaAnnotation, cache);
            put(name, internalJavaAnnotation);
            return annotationVisitor;
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            AnnotationVisitor annotationVisitor = super.visitArray(name);
            List<Object> list = new ArrayList<>(8);
            annotationVisitor = new ArrayVisitor(api, annotationVisitor, list, cache);
            put(name, list);
            return annotationVisitor;
        }
    }

    static class Visitor extends AbstractVisitor {

        private final InternalJavaAnnotation internalJavaAnnotation;

        Visitor(
                int api,
                AnnotationVisitor annotationVisitor,
                @NotNull InternalJavaAnnotation internalJavaAnnotation,
                @NotNull JavaCache cache) {
            super(api, annotationVisitor, cache);
            this.internalJavaAnnotation = internalJavaAnnotation;
        }

        protected void put(String name, Object value) {
            internalJavaAnnotation.mappings.put(name, value);
        }
    }

    private static class ArrayVisitor extends AbstractVisitor {

        private final List<Object> list;

        public ArrayVisitor(
                int api,
                AnnotationVisitor annotationVisitor,
                @NotNull List<Object> list,
                @NotNull JavaCache cache) {
            super(api, annotationVisitor, cache);
            Validation.notNull(list, "list must not be null.");
            this.list = list;
        }

        @Override
        protected void put(String name, Object value) {
            list.add(value);
        }
    }
}
