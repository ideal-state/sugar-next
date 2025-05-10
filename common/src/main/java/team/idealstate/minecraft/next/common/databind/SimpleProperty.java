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

package team.idealstate.minecraft.next.common.databind;

import lombok.Data;
import lombok.NonNull;
import team.idealstate.minecraft.next.common.string.StringUtils;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

@Data
final class SimpleProperty<V> implements Property<V> {
    @NonNull private final String key;
    private final V value;

    @Override
    public boolean isNull() {
        return isNull(getValue());
    }

    @Override
    public boolean isNotNull() {
        return isNotNull(getValue());
    }

    @Override
    public boolean isByte() {
        return isByte(getValue());
    }

    @Override
    public byte asByte() {
        return asByte(getValue());
    }

    @Override
    public boolean isShort() {
        return isShort(getValue());
    }

    @Override
    public short asShort() {
        return asShort(getValue());
    }

    @Override
    public boolean isInt() {
        return isInt(getValue());
    }

    @Override
    public int asInt() {
        return asInt(getValue());
    }

    @Override
    public boolean isLong() {
        return isLong(getValue());
    }

    @Override
    public long asLong() {
        return asLong(getValue());
    }

    @Override
    public boolean isFloat() {
        return isFloat(getValue());
    }

    @Override
    public float asFloat() {
        return asFloat(getValue());
    }

    @Override
    public boolean isDouble() {
        return isDouble(getValue());
    }

    @Override
    public double asDouble() {
        return asDouble(getValue());
    }

    @Override
    public boolean isBoolean() {
        return isBoolean(getValue());
    }

    @Override
    public boolean asBoolean() {
        return asBoolean(getValue());
    }

    @Override
    public boolean isChar() {
        return isChar(getValue());
    }

    @Override
    public char asChar() {
        return asChar(getValue());
    }

    @Override
    public boolean isString() {
        return isString(getValue());
    }

    @NotNull @Override
    public String asString() {
        return asString(getValue());
    }

    @Override
    public <T> boolean is(@NotNull Class<T> targetType) {
        return is(getValue(), targetType);
    }

    @NotNull @Override
    public <T> T as(@NotNull Class<T> targetType) {
        return as(getValue(), targetType);
    }

    private boolean isNull(V value) {
        return value == null;
    }

    private boolean isNotNull(V value) {
        return value != null;
    }

    private boolean isByte(V value) {
        if (isNull(value)) {
            return false;
        }
        if (isString(value)) {
            String string = asString(value);
            if (StringUtils.isInteger(string)) {
                try {
                    Byte.parseByte(string);
                    return true;
                } catch (NumberFormatException ignored) {
                }
            }
            return false;
        }
        return byte.class.isAssignableFrom(value.getClass()) || value instanceof Byte;
    }

    private byte asByte(V value) {
        Validation.is(isByte(), "value is not a byte.");
        return isString(value) ? Byte.parseByte(asString(value)) : (byte) value;
    }

    private boolean isShort(V value) {
        if (isNull(value)) {
            return false;
        }
        if (isString(value)) {
            String string = asString(value);
            if (StringUtils.isInteger(string)) {
                try {
                    Short.parseShort(string);
                    return true;
                } catch (NumberFormatException ignored) {
                }
            }
            return false;
        }
        return short.class.isAssignableFrom(value.getClass()) || value instanceof Short;
    }

    private short asShort(V value) {
        Validation.is(isShort(), "value is not a short.");
        return isString(value) ? Short.parseShort(asString(value)) : (short) value;
    }

    private boolean isInt(V value) {
        if (isNull(value)) {
            return false;
        }
        if (isString(value)) {
            String string = asString(value);
            if (StringUtils.isInteger(string)) {
                try {
                    Integer.parseInt(string);
                    return true;
                } catch (NumberFormatException ignored) {
                }
            }
            return false;
        }
        return int.class.isAssignableFrom(value.getClass()) || value instanceof Integer;
    }

    private int asInt(V value) {
        Validation.is(isInt(), "value is not a int.");
        return isString(value) ? Integer.parseInt(asString(value)) : (int) value;
    }

    private boolean isLong(V value) {
        if (isNull(value)) {
            return false;
        }
        if (isString(value)) {
            String string = asString(value);
            if (StringUtils.isInteger(string)) {
                try {
                    Long.parseLong(string);
                    return true;
                } catch (NumberFormatException ignored) {
                }
            }
            return false;
        }
        return long.class.isAssignableFrom(value.getClass()) || value instanceof Long;
    }

    private long asLong(V value) {
        Validation.is(isLong(), "value is not a long.");
        return isString(value) ? Long.parseLong(asString(value)) : (long) value;
    }

    private boolean isFloat(V value) {
        if (isNull(value)) {
            return false;
        }
        if (isString(value)) {
            String string = asString(value);
            if (StringUtils.isDecimal(string)) {
                try {
                    Float.parseFloat(string);
                    return true;
                } catch (NumberFormatException ignored) {
                }
            }
            return false;
        }
        return float.class.isAssignableFrom(value.getClass()) || value instanceof Float;
    }

    private float asFloat(V value) {
        Validation.is(isFloat(), "value is not a float.");
        return isString(value) ? Float.parseFloat(asString(value)) : (float) value;
    }

    private boolean isDouble(V value) {
        if (isNull(value)) {
            return false;
        }
        if (isString(value)) {
            String string = asString(value);
            if (StringUtils.isDecimal(string)) {
                try {
                    Double.parseDouble(string);
                    return true;
                } catch (NumberFormatException ignored) {
                }
            }
            return false;
        }
        return double.class.isAssignableFrom(value.getClass()) || value instanceof Double;
    }

    private double asDouble(V value) {
        Validation.is(isDouble(), "value is not a double.");
        return isString(value) ? Double.parseDouble(asString(value)) : (double) value;
    }

    private boolean isBoolean(V value) {
        if (isNull(value)) {
            return false;
        }
        if (isString(value)) {
            return StringUtils.isBoolean(asString(value));
        }
        return boolean.class.isAssignableFrom(value.getClass()) || value instanceof Boolean;
    }

    private boolean asBoolean(V value) {
        Validation.is(isBoolean(), "value is not a boolean.");
        return isString(value) ? StringUtils.isBoolean(asString(value)) : (boolean) value;
    }

    private boolean isChar(V value) {
        if (isNull(value)) {
            return false;
        }
        if (isString(value)) {
            return StringUtils.isChar(asString(value));
        }
        return char.class.isAssignableFrom(value.getClass()) || value instanceof Character;
    }

    private char asChar(V value) {
        Validation.is(isChar(), "value is not a char.");
        return isString(value) ? asString(value).charAt(0) : (char) value;
    }

    private boolean isString(V value) {
        return value instanceof String;
    }

    @NotNull private String asString(V value) {
        Validation.is(isString(value), "value is not a string.");
        return (String) value;
    }

    private <T> boolean is(V value, @NotNull Class<T> targetType) {
        Validation.notNull(targetType, "targetType must not be null.");
        if (isNull(value)) {
            return false;
        }
        return targetType.isAssignableFrom(value.getClass());
    }

    @NotNull private <T> T as(V value, @NotNull Class<T> targetType) {
        Validation.is(is(targetType), "value is not a " + targetType.getName() + ".");
        return targetType.cast(value);
    }
}
