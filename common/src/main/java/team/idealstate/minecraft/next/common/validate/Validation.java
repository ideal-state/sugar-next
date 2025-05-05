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

package team.idealstate.minecraft.next.common.validate;

import java.util.Objects;
import team.idealstate.minecraft.next.common.string.StringUtils;
import team.idealstate.minecraft.next.common.validate.exception.ValidationException;

public abstract class Validation {

    public static void vote(Boolean expression, String message) {
        if (!Boolean.TRUE.equals(expression)) {
            if (Objects.isNull(message)) {
                throw new ValidationException();
            }
            throw new ValidationException(message);
        }
    }

    public static <T> T requireNotNull(T object, String message) {
        notNull(object, message);
        return object;
    }

    public static void isNull(Object object, String message) {
        vote(Objects.isNull(object), message);
    }

    public static void notNull(Object object, String message) {
        vote(Objects.nonNull(object), message);
    }

    public static void isBlank(CharSequence string, String message) {
        vote(StringUtils.isBlank(string), message);
    }

    public static void isNullOrBlank(CharSequence string, String message) {
        vote(StringUtils.isNullOrBlank(string), message);
    }

    public static void notBlank(CharSequence string, String message) {
        vote(StringUtils.isNotBlank(string), message);
    }

    public static void notNullOrBlank(CharSequence string, String message) {
        vote(StringUtils.isNotNullOrBlank(string), message);
    }

    public static void isEmpty(CharSequence string, String message) {
        vote(StringUtils.isEmpty(string), message);
    }

    public static void isNullOrEmpty(CharSequence string, String message) {
        vote(StringUtils.isNullOrEmpty(string), message);
    }

    public static void notEmpty(CharSequence string, String message) {
        vote(StringUtils.isNotEmpty(string), message);
    }

    public static void notNullOrEmpty(CharSequence string, String message) {
        vote(StringUtils.isNotNullOrEmpty(string), message);
    }

    public static void isNumeric(CharSequence string, String message) {
        vote(StringUtils.isNumeric(string), message);
    }

    public static void notNumeric(CharSequence string, String message) {
        vote(StringUtils.isNotNumeric(string), message);
    }

    public static void isIntegral(CharSequence string, String message) {
        vote(StringUtils.isIntegral(string), message);
    }

    public static void notIntegral(CharSequence string, String message) {
        vote(StringUtils.isNotIntegral(string), message);
    }
}
