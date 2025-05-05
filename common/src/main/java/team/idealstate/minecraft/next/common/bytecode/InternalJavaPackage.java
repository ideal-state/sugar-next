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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;
import team.idealstate.minecraft.next.common.bytecode.api.member.JavaPackage;
import team.idealstate.minecraft.next.common.bytecode.api.struct.JavaAnnotation;
import team.idealstate.minecraft.next.common.bytecode.exception.BytecodeParsingException;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

class InternalJavaPackage implements JavaPackage {

    private static final Map<String, JavaPackage> PACKAGES_CACHE =
            new ConcurrentHashMap<>(64, 0.6F);
    private static final String PACKAGE_INFO = "package-info";
    private final String name;
    private final List<JavaAnnotation> annotations = new ArrayList<>(8);

    private InternalJavaPackage(@NotNull String name) {
        Validation.notNull(name, "name must not be null.");
        this.name = name;
    }

    @NotNull static JavaPackage newInstance(@NotNull String packageName) throws BytecodeParsingException {
        Validation.notNull(packageName, "packageName must not be null.");
        return PACKAGES_CACHE.computeIfAbsent(
                packageName,
                name -> {
                    String className = name + "." + PACKAGE_INFO;
                    InputStream inputStream =
                            ClassLoader.getSystemResourceAsStream(
                                    InternalJavaClass.internalize(className) + ".class");
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
                    Visitor visitor =
                            new Visitor(InternalJavaClass.ASM_API, null, internalJavaPackage);
                    classReader.accept(visitor, InternalJavaClass.ASM_PARSING_OPTIONS);
                    return internalJavaPackage;
                });
    }

    @NotNull @Override
    public String getName() {
        return name;
    }

    @NotNull @Override
    public JavaAnnotation[] getAnnotations() {
        return annotations.toArray(new JavaAnnotation[0]);
    }

    private static class Visitor extends ClassVisitor {

        private final InternalJavaPackage internalJavaPackage;

        private Visitor(
                int api, ClassVisitor cv, @NotNull InternalJavaPackage internalJavaPackage) {
            super(api, cv);
            Validation.notNull(internalJavaPackage, "internalJavaPackage must not be null.");
            this.internalJavaPackage = internalJavaPackage;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            AnnotationVisitor annotationVisitor = super.visitAnnotation(descriptor, visible);
            InternalJavaAnnotation internalJavaAnnotation =
                    new InternalJavaAnnotation(
                            Type.getType(descriptor).getClassName(), internalJavaPackage);
            annotationVisitor =
                    new InternalJavaAnnotation.Visitor(
                            api, annotationVisitor, internalJavaAnnotation);
            internalJavaPackage.annotations.add(internalJavaAnnotation);
            return annotationVisitor;
        }
    }
}
