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

package team.idealstate.sugar.next.hex;

import java.util.HashMap;
import java.util.Map;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;

public abstract class HexUtils {

    private static final char[] HEX_DIGITS = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };
    private static final Map<Character, Byte> HEX_DIGIT_TABLE = new HashMap<>(16);

    static {
        for (int i = 0; i < HEX_DIGITS.length; i++) {
            HEX_DIGIT_TABLE.put(HEX_DIGITS[i], (byte) i);
        }
    }

    @NotNull
    public static byte[] hexToBinary(@NotNull String hex) {
        Validation.notNull(hex, "hex must not be null");
        final int length = hex.length() / 2;
        byte[] binary = new byte[length];
        for (int i = 0; i < length; i++) {
            int j = i * 2;
            binary[i] = (byte) ((HEX_DIGIT_TABLE.get(hex.charAt(j)) << 4) + HEX_DIGIT_TABLE.get(hex.charAt(j + 1)));
        }
        return binary;
    }

    @NotNull
    public static String binaryToHex(@NotNull byte[] binary) {
        Validation.notNull(binary, "binary must not be null");
        StringBuilder builder = new StringBuilder();
        for (byte b : binary) {
            builder.append(HEX_DIGITS[(b & 0xf0) >> 4]).append(HEX_DIGITS[b & 0x0f]);
        }
        return builder.toString();
    }
}
