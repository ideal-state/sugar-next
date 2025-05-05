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

package team.idealstate.minecraft.next.common.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.Map;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

public abstract class Reflection {

    @SuppressWarnings("unchecked")
    @NotNull public static <T> T reflect(
            ClassLoader classLoader, @NotNull Class<T> reflectionInterface, Object target) {
        Validation.notNull(reflectionInterface, "reflectionInterface must not be null");
        Validation.vote(
                reflectionInterface.isInterface(), "reflectionInterface must be an interface");
        classLoader = classLoader != null ? classLoader : reflectionInterface.getClassLoader();
        InternalReflectionHandler internalReflectionHandler =
                new InternalReflectionHandler(classLoader, reflectionInterface, target);
        return (T)
                Proxy.newProxyInstance(
                        classLoader, new Class[] {reflectionInterface}, internalReflectionHandler);
    }

    @SuppressWarnings("unchecked")
    @NotNull public static <A extends Annotation> A annotation(
            @NotNull Class<A> annotationType, Map<String, Object> mappings) {
        Validation.notNull(annotationType, "annotationType must not be null");
        Validation.vote(
                Annotation.class.isAssignableFrom(annotationType),
                "annotationType must be an Annotation.");
        Validation.notNull(mappings, "mappings must not be null");

        return (A)
                Proxy.newProxyInstance(
                        annotationType.getClassLoader(),
                        new Class[] {annotationType},
                        new InternalAnnotationHandler(mappings));
    }
}
