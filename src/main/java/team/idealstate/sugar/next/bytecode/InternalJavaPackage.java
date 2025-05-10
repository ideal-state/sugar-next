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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import team.idealstate.sugar.internal.org.objectweb.asm.AnnotationVisitor;
import team.idealstate.sugar.internal.org.objectweb.asm.ClassReader;
import team.idealstate.sugar.internal.org.objectweb.asm.ClassVisitor;
import team.idealstate.sugar.internal.org.objectweb.asm.Type;
import team.idealstate.sugar.next.bytecode.api.member.JavaPackage;
import team.idealstate.sugar.next.bytecode.api.struct.JavaAnnotation;
import team.idealstate.sugar.next.bytecode.exception.BytecodeParsingException;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;

class InternalJavaPackage implements JavaPackage {

    private static final Map<String, JavaPackage> PACKAGES_CACHE = new ConcurrentHashMap<>(64, 0.6F);
    private static final String PACKAGE_INFO = "package-info";
    private final String name;
    private final List<JavaAnnotation> annotations = new ArrayList<>(8);

    private InternalJavaPackage(@NotNull String name) {
        Validation.notNull(name, "name must not be null.");
        this.name = name;
    }

    @NotNull
    static JavaPackage newInstance(@NotNull String packageName, @NotNull JavaCache cache)
            throws BytecodeParsingException {
        Validation.notNull(packageName, "packageName must not be null.");
        Validation.notNull(cache, "cache must not be null.");
        return PACKAGES_CACHE.computeIfAbsent(packageName, name -> {
            String className = name + "." + PACKAGE_INFO;
            InputStream inputStream =
                    ClassLoader.getSystemResourceAsStream(InternalJavaClass.internalize(className) + ".class");
            InternalJavaPackage internalJavaPackage = new InternalJavaPackage(packageName);
            if (inputStream == null) {
                return internalJavaPackage;
            }
            ClassReader classReader;
            try {
                classReader = new ClassReader(inputStream);
            } catch (IOException e) {
                throw new BytecodeParsingException(e);
            }
            Visitor visitor = new Visitor(InternalJavaClass.ASM_API, null, internalJavaPackage, cache);
            classReader.accept(visitor, InternalJavaClass.ASM_PARSING_OPTIONS);
            return internalJavaPackage;
        });
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public JavaAnnotation[] getAnnotations() {
        return annotations.toArray(new JavaAnnotation[0]);
    }

    private static class Visitor extends ClassVisitor {

        private final InternalJavaPackage internalJavaPackage;

        private final JavaCache cache;

        private Visitor(
                int api, ClassVisitor cv, @NotNull InternalJavaPackage internalJavaPackage, @NotNull JavaCache cache) {
            super(api, cv);
            Validation.notNull(internalJavaPackage, "internalJavaPackage must not be null.");
            Validation.notNull(cache, "cache must not be null.");
            this.internalJavaPackage = internalJavaPackage;
            this.cache = cache;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            AnnotationVisitor annotationVisitor = super.visitAnnotation(descriptor, visible);
            InternalJavaAnnotation internalJavaAnnotation =
                    new InternalJavaAnnotation(Type.getType(descriptor).getClassName(), internalJavaPackage, cache);
            annotationVisitor =
                    new InternalJavaAnnotation.Visitor(api, annotationVisitor, internalJavaAnnotation, cache);
            internalJavaPackage.annotations.add(internalJavaAnnotation);
            return annotationVisitor;
        }
    }
}
