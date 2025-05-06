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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import team.idealstate.minecraft.next.common.bytecode.api.JavaType;
import team.idealstate.minecraft.next.common.bytecode.api.member.JavaClass;
import team.idealstate.minecraft.next.common.bytecode.api.member.JavaConstructor;
import team.idealstate.minecraft.next.common.bytecode.api.member.JavaField;
import team.idealstate.minecraft.next.common.bytecode.api.member.JavaMethod;
import team.idealstate.minecraft.next.common.bytecode.api.member.JavaPackage;
import team.idealstate.minecraft.next.common.bytecode.api.struct.JavaAnnotation;
import team.idealstate.minecraft.next.common.bytecode.exception.BytecodeParsingException;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;
import team.idealstate.minecraft.next.common.validate.annotation.Nullable;

class InternalJavaClass implements JavaClass {

    static final int ASM_API = Opcodes.ASM9;
    static final int ASM_PARSING_OPTIONS = ClassReader.SKIP_FRAMES | ClassReader.EXPAND_FRAMES;
    private final Collection<String> innerClassNames = new ArrayList<>(8);
    private final List<JavaConstructor> constructors = new ArrayList<>(8);
    private final List<JavaField> fields = new ArrayList<>(16);
    private final List<JavaMethod> methods = new ArrayList<>(16);
    private final Set<JavaType> cannotBeAssignableFrom = new CopyOnWriteArraySet<>();
    private final List<JavaAnnotation> annotations = new ArrayList<>(8);
    private String packageName;
    private String superClassName;
    private String[] interfaceNames;
    private String outerClassName;
    private String name;
    private int access;
    private int version;
    private final JavaCache cache;

    protected InternalJavaClass(@NotNull JavaCache cache) {
        Validation.notNull(cache, "cache must not be null.");
        this.cache = cache;
    }

    static String internalize(String name) {
        return name.replace('.', '/');
    }

    static String normalize(String name) {
        return name.replace('/', '.');
    }

    static JavaClass newInstance(
            @NotNull String className, @NotNull InputStream inputStream, @NotNull JavaCache cache)
            throws BytecodeParsingException {
        Validation.notNull(className, "className must not be null.");
        Validation.notNull(inputStream, "inputStream must not be null.");
        Validation.notNull(cache, "cache must not be null.");
        ClassReader classReader;
        try {
            classReader = new ClassReader(inputStream);
        } catch (IOException e) {
            throw new BytecodeParsingException(e);
        }

        InternalJavaClass internalJavaClass = new InternalJavaClass(cache);
        cache.put(className, internalJavaClass);
        Visitor visitor = new Visitor(ASM_API, null, internalJavaClass, cache);
        classReader.accept(visitor, ASM_PARSING_OPTIONS);

        return internalJavaClass;
    }

    static JavaClass newInstance(
            @NotNull String className, @NotNull byte[] classFile, @NotNull JavaCache cache)
            throws BytecodeParsingException {
        Validation.notNull(className, "className must not be null.");
        Validation.notNull(classFile, "classFile must not be null.");
        Validation.notNull(cache, "cache must not be null.");
        ClassReader classReader = new ClassReader(classFile);

        InternalJavaClass internalJavaClass = new InternalJavaClass(cache);
        cache.put(className, internalJavaClass);
        Visitor visitor = new Visitor(ASM_API, null, internalJavaClass, cache);
        classReader.accept(visitor, ASM_PARSING_OPTIONS);

        return internalJavaClass;
    }

    @NotNull @Override
    public JavaPackage getPackage() {
        return InternalJavaPackage.newInstance(packageName, cache);
    }

    @Nullable @Override
    public JavaClass getSuperClass() {
        return typeof(superClassName, cache);
    }

    @NotNull @Override
    public JavaClass[] getInterfaces() {
        return Arrays.stream(interfaceNames).map(cn -> typeof(cn, cache)).toArray(JavaClass[]::new);
    }

    @Nullable @Override
    public JavaClass getOuterClass() {
        return typeof(outerClassName, cache);
    }

    @NotNull @Override
    public JavaClass[] getInnerClasses() {
        return innerClassNames.stream().map(cn -> typeof(cn, cache)).toArray(JavaClass[]::new);
    }

    @NotNull @Override
    public JavaConstructor[] getConstructors() {
        return constructors.toArray(new JavaConstructor[0]);
    }

    @NotNull @Override
    public JavaField[] getFields() {
        return fields.toArray(new JavaField[0]);
    }

    @NotNull @Override
    public JavaMethod[] getMethods() {
        return methods.toArray(new JavaMethod[0]);
    }

    @NotNull @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isAssignableFrom(@NotNull JavaType type) {
        Validation.notNull(type, "type must not be null.");
        Validation.is(type instanceof JavaClass, "type must be a JavaClass.");
        if (cannotBeAssignableFrom.contains(type)) {
            return false;
        }
        if (this.equals(type)) {
            return true;
        }
        assert type instanceof JavaClass;
        JavaClass that = (JavaClass) type;

        JavaClass superClass = that.getSuperClass();
        if (superClass != null && isAssignableFrom(superClass)) {
            return true;
        }
        JavaClass[] interfaces = that.getInterfaces();
        for (JavaClass anInterface : interfaces) {
            if (isAssignableFrom(anInterface)) {
                return true;
            }
        }

        cannotBeAssignableFrom.add(type);
        return false;
    }

    @Override
    public int getAccess() {
        return access;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @NotNull @Override
    public JavaAnnotation[] getAnnotations() {
        return annotations.toArray(new JavaAnnotation[0]);
    }

    private static class Visitor extends ClassVisitor {

        private static final String STATIC_INIT_METHOD_NAME = "<cinit>";
        private static final String INIT_METHOD_NAME = "<init>";
        private final InternalJavaClass internalJavaClass;
        private final JavaCache cache;

        private Visitor(
                int api,
                ClassVisitor cv,
                @NotNull InternalJavaClass internalJavaClass,
                @NotNull JavaCache cache) {
            super(api, cv);
            Validation.notNull(internalJavaClass, "internalJavaClass must not be null.");
            Validation.notNull(cache, "cache must not be null.");
            this.internalJavaClass = internalJavaClass;
            this.cache = cache;
        }

        @Override
        public void visit(
                int version,
                int access,
                String name,
                String signature,
                String superName,
                String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            internalJavaClass.version = version;
            internalJavaClass.access = access;
            internalJavaClass.packageName = normalize(name.substring(0, name.lastIndexOf('/')));
            internalJavaClass.name = normalize(name);
            internalJavaClass.superClassName = normalize(superName);
            internalJavaClass.interfaceNames =
                    Arrays.stream(interfaces)
                            .map(InternalJavaClass::normalize)
                            .toArray(String[]::new);
        }

        @Override
        public void visitOuterClass(String owner, String name, String descriptor) {
            super.visitOuterClass(owner, name, descriptor);
            internalJavaClass.outerClassName = normalize(owner);
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            super.visitInnerClass(name, outerName, innerName, access);
            internalJavaClass.innerClassNames.add(normalize(name));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            AnnotationVisitor annotationVisitor = super.visitAnnotation(descriptor, visible);
            InternalJavaAnnotation internalJavaAnnotation =
                    new InternalJavaAnnotation(
                            Type.getType(descriptor).getClassName(), internalJavaClass, cache);
            annotationVisitor =
                    new InternalJavaAnnotation.Visitor(
                            api, annotationVisitor, internalJavaAnnotation, cache);
            internalJavaClass.annotations.add(internalJavaAnnotation);
            return annotationVisitor;
        }

        @Override
        public FieldVisitor visitField(
                int access, String name, String descriptor, String signature, Object value) {
            FieldVisitor fieldVisitor =
                    super.visitField(access, name, descriptor, signature, value);
            InternalJavaField internalJavaField =
                    new InternalJavaField(
                            internalJavaClass,
                            access,
                            name,
                            Type.getType(descriptor).getClassName(),
                            value,
                            cache);
            fieldVisitor =
                    new InternalJavaField.Visitor(api, fieldVisitor, internalJavaField, cache);
            internalJavaClass.fields.add(internalJavaField);
            return fieldVisitor;
        }

        @Override
        public MethodVisitor visitMethod(
                int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor methodVisitor =
                    super.visitMethod(access, name, descriptor, signature, exceptions);
            switch (name) {
                case STATIC_INIT_METHOD_NAME:
                    break;
                case INIT_METHOD_NAME:
                    InternalJavaMethod delegate =
                            new InternalJavaMethod(
                                    internalJavaClass,
                                    access,
                                    name,
                                    Type.getArgumentCount(descriptor),
                                    Type.getReturnType(descriptor).getClassName(),
                                    exceptions == null
                                            ? null
                                            : Arrays.stream(exceptions)
                                                    .map(InternalJavaClass::normalize)
                                                    .toArray(String[]::new),
                                    cache);
                    InternalJavaConstructor internalJavaConstructor =
                            new InternalJavaConstructor(delegate);
                    methodVisitor =
                            new InternalJavaMethod.Visitor(api, methodVisitor, delegate, cache);
                    internalJavaClass.constructors.add(internalJavaConstructor);
                    break;
                default:
                    InternalJavaMethod internalJavaMethod =
                            new InternalJavaMethod(
                                    internalJavaClass,
                                    access,
                                    name,
                                    Type.getArgumentCount(descriptor),
                                    Type.getReturnType(descriptor).getClassName(),
                                    exceptions == null
                                            ? null
                                            : Arrays.stream(exceptions)
                                                    .map(InternalJavaClass::normalize)
                                                    .toArray(String[]::new),
                                    cache);
                    methodVisitor =
                            new InternalJavaMethod.Visitor(
                                    api, methodVisitor, internalJavaMethod, cache);
                    internalJavaClass.methods.add(internalJavaMethod);
            }
            return methodVisitor;
        }
    }
}
