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

package team.idealstate.sugar.next.reflect;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import team.idealstate.sugar.next.reflect.annotation.Reflect;
import team.idealstate.sugar.next.reflect.annotation.ReflectConstructor;
import team.idealstate.sugar.next.reflect.annotation.ReflectField;
import team.idealstate.sugar.next.reflect.annotation.ReflectMethod;
import team.idealstate.sugar.next.reflect.exception.ReflectionException;
import team.idealstate.sugar.string.StringUtils;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;

class InternalReflectionHandler implements ReflectionInvocationHandler {

    private static final MethodHandles.Lookup PUBLIC_LOOKUP = InternalMethodHandles.publicLookup();
    private static final MethodHandles.Lookup LOOKUP = InternalMethodHandles.lookup();

    private final Object target;
    private final Reflect reflect;
    private final Map<String, InvocationHandler> handlers = new ConcurrentHashMap<>(16, 0.6F);
    private volatile MethodHandles.Lookup specialLookup = null;

    InternalReflectionHandler(@NotNull ClassLoader classLoader, @NotNull Class<?> reflectionInterface, Object target) {
        Validation.notNull(classLoader, "classLoader must not be null");
        Validation.notNull(reflectionInterface, "reflectionInterface must not be null");
        Validation.is(reflectionInterface.isInterface(), "reflectionInterface must be an interface");
        this.target = target;

        this.reflect = reflectionInterface.getDeclaredAnnotation(Reflect.class);
    }

    @NotNull
    private static Class<?> getDeclaringClass(
            Class<?> cls, String name, ClassLoader classLoader, Reflect defaultClass) {
        if (cls == null || void.class.equals(cls)) {
            try {
                return Class.forName(name, false, classLoader);
            } catch (ClassNotFoundException e) {
                if (defaultClass == null) {
                    throw new ReflectionException(e);
                }
                return getDeclaringClass(defaultClass.value(), defaultClass.name(), classLoader, null);
            }
        }
        return cls;
    }

    @NotNull
    private static MethodHandles.Lookup lookup(boolean accessible) {
        return accessible ? LOOKUP : PUBLIC_LOOKUP;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object ret = ReflectionInvocationHandler.super.invoke(proxy, method, args);
        if (ret != null) {
            return ret;
        }
        String methodDesc = method.toString();
        InvocationHandler handler = handlers.computeIfAbsent(methodDesc, key -> {
            Annotation[] annotations = method.getDeclaredAnnotations();
            for (Annotation annotation : annotations) {
                Class<? extends Annotation> annotationType = annotation.annotationType();
                if (ReflectConstructor.class.equals(annotationType)) {
                    ReflectConstructor reflectConstructor = (ReflectConstructor) annotation;
                    return new ReflectConstructorHandler(reflect, reflectConstructor);
                } else if (ReflectField.class.equals(annotationType)) {
                    ReflectField reflectField = (ReflectField) annotation;
                    return new ReflectFieldHandler(reflect, reflectField, target);
                } else if (ReflectMethod.class.equals(annotationType)) {
                    ReflectMethod reflectMethod = (ReflectMethod) annotation;
                    return new ReflectMethodHandler(reflect, reflectMethod, target);
                }
            }
            if (method.isDefault()) {
                if (specialLookup == null) {
                    this.specialLookup = InternalMethodHandles.privateLookup(method.getDeclaringClass());
                }
                return new DefaultMethodHandler(specialLookup);
            }
            return UnimplementedMethodHandler.INSTANCE;
        });
        return handler.invoke(proxy, method, args);
    }

    private enum UnimplementedMethodHandler implements InvocationHandler {
        INSTANCE;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            throw new ReflectionException("Unimplemented reflection interface.");
        }
    }

    private static class DefaultMethodHandler implements InvocationHandler {

        private final MethodHandles.Lookup specialLookup;
        private volatile MethodHandle defaultMethod = null;

        private DefaultMethodHandler(MethodHandles.Lookup specialLookup) {
            this.specialLookup = specialLookup;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (defaultMethod == null) {
                synchronized (this) {
                    if (defaultMethod == null) {
                        try {
                            MethodHandle methodHandle =
                                    specialLookup.unreflectSpecial(method, method.getDeclaringClass());
                            this.defaultMethod = methodHandle.bindTo(proxy);
                        } catch (ReflectiveOperationException e) {
                            throw new ReflectionException(e);
                        }
                    }
                }
            }
            return defaultMethod.invokeWithArguments(args);
        }
    }

    private static class ReflectConstructorHandler implements InvocationHandler {

        private final Reflect reflect;
        private final ReflectConstructor reflectConstructor;
        private volatile MethodHandle constructor = null;

        private ReflectConstructorHandler(Reflect reflect, ReflectConstructor reflectConstructor) {
            this.reflect = reflect;
            this.reflectConstructor = reflectConstructor;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (constructor == null) {
                synchronized (this) {
                    if (constructor == null) {
                        try {
                            Class<?> declaringClass = InternalReflectionHandler.getDeclaringClass(
                                    reflectConstructor.value(),
                                    reflectConstructor.declaringClass(),
                                    method.getDeclaringClass().getClassLoader(),
                                    this.reflect);

                            if (!method.getReturnType().isAssignableFrom(declaringClass)) {
                                throw new ReflectionException(
                                        "Return type of constructor must be assignable from target"
                                                + " declaring class.");
                            }

                            MethodType methodType = MethodType.methodType(void.class, method.getParameterTypes());
                            this.constructor = InternalReflectionHandler.lookup(reflectConstructor.accessible())
                                    .findConstructor(declaringClass, methodType);
                        } catch (ReflectiveOperationException e) {
                            throw new ReflectionException(e);
                        }
                    }
                }
            }
            return constructor.invokeWithArguments(args);
        }
    }

    private static class ReflectFieldHandler implements InvocationHandler {

        private static final Field MODIFIERS_FIELD;

        static {
            try {
                MODIFIERS_FIELD = Field.class.getDeclaredField("modifiers");
                MODIFIERS_FIELD.setAccessible(true);
            } catch (NoSuchFieldException e) {
                throw new ReflectionException(e);
            }
        }

        private final Reflect reflect;
        private final ReflectField reflectField;
        private final Object target;
        private volatile Field field = null;
        private volatile MethodHandle getter = null;
        private volatile MethodHandle setter = null;

        private ReflectFieldHandler(Reflect reflect, ReflectField reflectField, Object target) {
            this.reflect = reflect;
            this.reflectField = reflectField;
            this.target = reflectField.statical() ? null : target;
        }

        private static boolean isSetter(Method method, Object[] args, Field field) {
            boolean isSetter = false;
            if (args != null && args.length > 0) {
                if (args.length != 1) {
                    throw new ReflectionException("Field setter only one argument is allowed.");
                }
                if (!field.getType().isAssignableFrom(method.getParameterTypes()[0])) {
                    throw new ReflectionException("Field type must be assignable from argument type.");
                }
                isSetter = true;
            }
            return isSetter;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (field == null) {
                synchronized (this) {
                    if (field == null) {
                        try {
                            Class<?> declaringClass = getDeclaringClass(
                                    reflectField.value(),
                                    reflectField.declaringClass(),
                                    proxy.getClass().getClassLoader(),
                                    this.reflect);
                            String memberName = reflectField.name();
                            if (StringUtils.isEmpty(memberName)) {
                                memberName = method.getName();
                            }
                            Field field = declaringClass.getDeclaredField(memberName);
                            boolean isGetter = !void.class.equals(method.getReturnType());
                            boolean isSetter = isSetter(method, args, field);
                            if (isGetter && !method.getReturnType().isAssignableFrom(field.getType())) {
                                throw new ReflectionException(
                                        "Return type of field must be assignable from target field" + " type.");
                            }
                            boolean accessible = reflectField.accessible();
                            if (accessible) {
                                field.setAccessible(true);
                                int modifiers = field.getModifiers();
                                if (reflectField.statical() && Modifier.isFinal(modifiers)) {
                                    MODIFIERS_FIELD.setInt(field, modifiers & ~Modifier.FINAL);
                                }
                            }
                            MethodHandles.Lookup lookup = InternalReflectionHandler.lookup(accessible);
                            if (reflectField.statical()) {
                                if (isGetter) {
                                    this.getter = lookup.unreflectGetter(field);
                                }
                                if (isSetter) {
                                    this.setter = lookup.unreflectSetter(field);
                                }
                            } else {
                                if (isGetter) {
                                    this.getter = lookup.unreflectGetter(field).bindTo(target);
                                }
                                if (isSetter) {
                                    this.setter = lookup.unreflectSetter(field).bindTo(target);
                                }
                            }
                            this.field = field;
                        } catch (ReflectiveOperationException e) {
                            throw new ReflectionException(e);
                        }
                    }
                }
            }
            Object ret = null;
            if (getter != null) {
                ret = getter.invoke();
            }
            if (setter != null) {
                assert args != null && args.length == 1;
                setter.invokeWithArguments(args[0]);
            }
            return ret;
        }
    }

    private static class ReflectMethodHandler implements InvocationHandler {
        private final Reflect reflect;
        private final ReflectMethod reflectMethod;
        private final Object target;
        private volatile MethodHandle methodHandle = null;

        private ReflectMethodHandler(Reflect reflect, ReflectMethod reflectMethod, Object target) {
            this.reflect = reflect;
            this.reflectMethod = reflectMethod;
            this.target = reflectMethod.statical() ? null : target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (methodHandle == null) {
                synchronized (this) {
                    if (methodHandle == null) {
                        try {
                            Class<?> declaringClass = getDeclaringClass(
                                    reflectMethod.value(),
                                    reflectMethod.declaringClass(),
                                    proxy.getClass().getClassLoader(),
                                    this.reflect);
                            String memberName = reflectMethod.name();
                            if (StringUtils.isEmpty(memberName)) {
                                memberName = method.getName();
                            }
                            Method _method = declaringClass.getDeclaredMethod(memberName, method.getParameterTypes());
                            if (!method.getReturnType().isAssignableFrom(_method.getReturnType())) {
                                throw new ReflectionException("Return type of method must be assignable from target"
                                        + " method return type.");
                            }
                            boolean accessible = reflectMethod.accessible();
                            if (accessible) {
                                _method.setAccessible(true);
                            }
                            MethodHandles.Lookup lookup = InternalReflectionHandler.lookup(accessible);
                            if (reflectMethod.statical()) {
                                this.methodHandle = lookup.unreflect(_method);
                            } else {
                                this.methodHandle = lookup.unreflect(_method).bindTo(target);
                            }
                        } catch (ReflectiveOperationException e) {
                            throw new ReflectionException(e);
                        }
                    }
                }
            }
            return methodHandle.invokeWithArguments(args);
        }
    }
}
