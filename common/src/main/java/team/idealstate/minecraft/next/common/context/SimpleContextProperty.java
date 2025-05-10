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

package team.idealstate.minecraft.next.common.context;

import lombok.Data;
import team.idealstate.minecraft.next.common.databind.Property;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

@Data
final class SimpleContextProperty implements ContextProperty {

    @NotNull private final Property<String> property;

    @NotNull @Override
    public String getKey() {
        return property.getKey();
    }

    @NotNull @Override
    public String getValue() {
        return Validation.requireNotNull(property.getValue(), "value must not be null.");
    }

    @Override
    public boolean isNull() {
        return property.isNull();
    }

    @Override
    public boolean isNotNull() {
        return property.isNotNull();
    }

    @Override
    public boolean isByte() {
        return property.isByte();
    }

    @Override
    public byte asByte() {
        return property.asByte();
    }

    @Override
    public boolean isShort() {
        return property.isShort();
    }

    @Override
    public short asShort() {
        return property.asShort();
    }

    @Override
    public boolean isInt() {
        return property.isInt();
    }

    @Override
    public int asInt() {
        return property.asInt();
    }

    @Override
    public boolean isLong() {
        return property.isLong();
    }

    @Override
    public long asLong() {
        return property.asLong();
    }

    @Override
    public boolean isFloat() {
        return property.isFloat();
    }

    @Override
    public float asFloat() {
        return property.asFloat();
    }

    @Override
    public boolean isDouble() {
        return property.isDouble();
    }

    @Override
    public double asDouble() {
        return property.asDouble();
    }

    @Override
    public boolean isBoolean() {
        return property.isBoolean();
    }

    @Override
    public boolean asBoolean() {
        return property.asBoolean();
    }

    @Override
    public boolean isChar() {
        return property.isChar();
    }

    @Override
    public char asChar() {
        return property.asChar();
    }

    @Override
    public boolean isString() {
        return property.isString();
    }

    @NotNull @Override
    public String asString() {
        return property.asString();
    }

    @Override
    public <T> boolean is(@NotNull Class<T> targetType) {
        return property.is(targetType);
    }

    @NotNull @Override
    public <T> T as(@NotNull Class<T> targetType) {
        return property.as(targetType);
    }
}
