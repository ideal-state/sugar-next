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

package team.idealstate.sugar.next.calculate;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public final class Variable implements Cloneable {
    private static final Set<Character> NAMING_HEADER_RULE;
    private static final Set<Character> NAMING_RULE;
    private static final Pattern CHINESE_CHAR = Pattern.compile("[\\u4e00-\\u9fa5]");

    static {
        List<Character> a2z = Arrays.asList(
                'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u',
                'v', 'w', 'x', 'y', 'z');
        List<Character> A2Z = Arrays.asList(
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U',
                'V', 'W', 'X', 'Y', 'Z');
        List<Character> o29 = Arrays.asList('0', '1', '2', '3', '4', '5', '6', '7', '8', '9');
        List<Character> other = Arrays.asList('_', '$');
        Set<Character> rule = new HashSet<>(a2z.size() + A2Z.size() + other.size());
        rule.addAll(a2z);
        rule.addAll(A2Z);
        rule.addAll(other);
        NAMING_HEADER_RULE = Collections.unmodifiableSet(rule);
        rule = new HashSet<>(NAMING_HEADER_RULE.size() + o29.size());
        rule.addAll(NAMING_HEADER_RULE);
        rule.addAll(o29);
        NAMING_RULE = Collections.unmodifiableSet(rule);
    }

    private final String name;
    private final Number value;

    public Variable(CharSequence name, Number value) {
        validateName(name);
        this.name = name instanceof String ? (String) name : name.toString();
        this.value = value;
    }

    public static boolean isNameHeader(char header) {
        return NAMING_HEADER_RULE.contains(header)
                || CHINESE_CHAR.matcher(String.valueOf(header)).find();
    }

    public static boolean isNameContent(char content) {
        return NAMING_RULE.contains(content)
                || CHINESE_CHAR.matcher(String.valueOf(content)).find();
    }

    public static void validateName(CharSequence name) {
        if (name == null || name.length() == 0 || !isNameHeader(name.charAt(0))) {
            throw new IllegalArgumentException("Invalid variable name [" + name + "]! (column: 0)");
        }
        for (int i = 1; i < name.length(); i++) {
            if (!isNameContent(name.charAt(i))) {
                throw new IllegalArgumentException("Invalid variable name [" + name + "]! (column: " + i + ")");
            }
        }
    }

    public Number valueOf(Map<String, Number> context) {
        if (name == null) {
            return getValue();
        }
        Number number = context.get(name);
        if (number != null) {
            return number;
        }
        return getValue();
    }

    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public Variable clone() {
        return new Variable(name, value);
    }
}
